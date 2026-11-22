import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, DestroyRef, ElementRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { ApiError } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { Income, RecurringExpense } from '../../core/planning/planning.models';
import { PlanningService } from '../../core/planning/planning.service';
import { ModalComponent } from '../../shared/modal/modal.component';
import { PrivateHeaderComponent } from '../../shared/private-header/private-header.component';
import { parseAmount } from '../movements/movements.component';

const amountValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  parseAmount(String(control.value ?? '')) === null ? { amount: true } : null;

const dueDayValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const raw = String(control.value ?? '').trim();
  if (!/^\d{1,2}$/.test(raw)) return { dueDay: true };
  const day = Number(raw);
  return Number.isInteger(day) && day >= 1 && day <= 31 ? null : { dueDay: true };
};

@Component({
  selector: 'app-planning',
  imports: [ReactiveFormsModule, RouterLink, PrivateHeaderComponent, ModalComponent],
  templateUrl: './planning.component.html',
  styleUrl: './planning.component.scss',
})
export class PlanningComponent implements OnInit {
  readonly incomeEditorOpen = signal(false);
  readonly incomeDeleteOpen = signal(false);
  readonly expenseEditorOpen = signal(false);
  readonly expenseDeleteOpen = signal(false);

  editingIncome?: Income;
  deletingIncome?: Income;
  editingExpense?: RecurringExpense;
  deletingExpense?: RecurringExpense;

  private readonly incomeFormElement = viewChild<ElementRef<HTMLFormElement>>('incomeFormElement');
  private readonly expenseFormElement = viewChild<ElementRef<HTMLFormElement>>('expenseFormElement');

  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly planningService = inject(PlanningService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private loadSequence = 0;

  readonly incomeForm = this.formBuilder.group({
    fonte: this.formBuilder.control('', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)]),
    valorBase: this.formBuilder.control('', [Validators.required, amountValidator]),
    ativo: this.formBuilder.control(true),
  });

  readonly expenseForm = this.formBuilder.group({
    descricao: this.formBuilder.control('', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(150)]),
    valor: this.formBuilder.control('', [Validators.required, amountValidator]),
    diaVencimento: this.formBuilder.control<string | number | null>('10', [Validators.required, dueDayValidator]),
    ativo: this.formBuilder.control(true),
  });

  incomes: Income[] = [];
  recurringExpenses: RecurringExpense[] = [];

  isLoading = true;
  isSavingIncome = false;
  isDeletingIncome = false;
  isSavingExpense = false;
  isDeletingExpense = false;
  togglingIncomeId = '';
  togglingExpenseId = '';

  incomeSubmitted = false;
  expenseSubmitted = false;

  loadError = '';
  incomeSaveError = '';
  incomeDeleteError = '';
  expenseSaveError = '';
  expenseDeleteError = '';
  sessionError = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadPlanning();
  }

  get activeIncomesCents(): number {
    return this.incomes
      .filter((income) => income.ativo)
      .reduce((cents, income) => cents + Math.round(income.valorBase * 100), 0);
  }

  get activeExpensesCents(): number {
    return this.recurringExpenses
      .filter((expense) => expense.ativo)
      .reduce((cents, expense) => cents + Math.round(expense.valor * 100), 0);
  }

  get totalActiveIncomes(): number {
    return this.activeIncomesCents / 100;
  }

  get totalActiveExpenses(): number {
    return this.activeExpensesCents / 100;
  }

  get freeMargin(): number {
    return (this.activeIncomesCents - this.activeExpensesCents) / 100;
  }

  get commitmentPercent(): number {
    if (this.activeIncomesCents <= 0) {
      return this.activeExpensesCents > 0 ? 100 : 0;
    }
    return Math.round((this.activeExpensesCents / this.activeIncomesCents) * 100);
  }

  get sortedRecurringExpenses(): RecurringExpense[] {
    return [...this.recurringExpenses].sort(
      (a, b) => a.diaVencimento - b.diaVencimento || a.descricao.localeCompare(b.descricao, 'pt-BR'),
    );
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  }

  formatSignedMoney(value: number): string {
    const sign = value > 0 ? '+ ' : value < 0 ? '− ' : '';
    return sign + this.formatMoney(Math.abs(value));
  }

  formatIncomeAmountField(): void {
    const amount = parseAmount(this.incomeForm.controls.valorBase.value);
    if (amount === null) return;
    this.incomeForm.controls.valorBase.setValue(
      new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount),
    );
  }

  formatExpenseAmountField(): void {
    const amount = parseAmount(this.expenseForm.controls.valor.value);
    if (amount === null) return;
    this.expenseForm.controls.valor.setValue(
      new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount),
    );
  }

  incomeFieldInvalid(name: 'fonte' | 'valorBase'): boolean {
    const control = this.incomeForm.controls[name];
    return control.invalid && (control.touched || this.incomeSubmitted);
  }

  expenseFieldInvalid(name: 'descricao' | 'valor' | 'diaVencimento'): boolean {
    const control = this.expenseForm.controls[name];
    return control.invalid && (control.touched || this.expenseSubmitted);
  }

  loadPlanning(): void {
    const sequence = ++this.loadSequence;
    this.isLoading = true;
    this.loadError = '';
    forkJoin({
      incomes: this.planningService.listIncomes(),
      recurringExpenses: this.planningService.listRecurringExpenses(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ incomes, recurringExpenses }) => {
          if (sequence !== this.loadSequence) return;
          this.incomes = incomes;
          this.recurringExpenses = recurringExpenses;
          this.isLoading = false;
          this.changeDetector.markForCheck();
        },
        error: (error: HttpErrorResponse) => {
          if (sequence !== this.loadSequence) return;
          this.isLoading = false;
          this.changeDetector.markForCheck();
          if (error.status === 401) {
            void this.router.navigate(['/login']);
            return;
          }
          this.loadError =
            error.status === 0
              ? 'Não conseguimos carregar seu planejamento. Confira sua conexão e tente novamente.'
              : 'Não foi possível carregar seu planejamento agora. Tente novamente.';
        },
      });
  }

  openIncomeEditor(income?: Income): void {
    this.editingIncome = income;
    this.successMessage = '';
    this.incomeForm.reset({
      fonte: income?.fonte ?? '',
      valorBase: income ? income.valorBase.toFixed(2).replace('.', ',') : '',
      ativo: income?.ativo ?? true,
    });
    this.incomeSubmitted = false;
    this.incomeSaveError = '';
    this.incomeEditorOpen.set(true);
  }

  closeIncomeEditor(): void {
    if (this.isSavingIncome) return;
    this.incomeEditorOpen.set(false);
    this.incomeSaveError = '';
  }

  saveIncome(): void {
    if (this.isSavingIncome) return;
    this.incomeSubmitted = true;
    this.incomeSaveError = '';
    if (this.incomeForm.invalid) {
      this.incomeForm.markAllAsTouched();
      this.incomeFormElement()?.nativeElement.querySelector<HTMLElement>('input.ng-invalid')?.focus();
      return;
    }

    const value = this.incomeForm.getRawValue();
    const amount = parseAmount(value.valorBase);
    if (amount === null) return;

    this.isSavingIncome = true;
    const request = {
      fonte: value.fonte.trim(),
      valorBase: amount,
      ativo: value.ativo,
    };

    const action$ = this.editingIncome
      ? this.planningService.updateIncome(this.editingIncome.id, request)
      : this.planningService.createIncome(request);

    action$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isSavingIncome = false;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.isSavingIncome = false;
          this.closeIncomeEditor();
          this.successMessage = this.editingIncome
            ? 'Fonte de renda atualizada com sucesso.'
            : 'Fonte de renda cadastrada com sucesso.';
          this.loadPlanning();
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 401) {
            void this.router.navigate(['/login']);
            return;
          }
          const apiError = error.error as ApiError | undefined;
          this.incomeSaveError =
            error.status === 0
              ? 'Não foi possível confirmar o registro. Confira sua conexão e tente novamente.'
              : Object.values(apiError?.campos ?? {})[0] ??
                apiError?.mensagem ??
                'Não foi possível salvar a fonte de renda. Tente novamente.';
        },
      });
  }

  toggleIncomeActive(income: Income): void {
    if (this.togglingIncomeId) return;
    this.togglingIncomeId = income.id;
    this.successMessage = '';
    this.planningService
      .updateIncome(income.id, {
        fonte: income.fonte,
        valorBase: income.valorBase,
        ativo: !income.ativo,
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.togglingIncomeId = '';
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: (updated) => {
          this.incomes = this.incomes.map((item) => (item.id === updated.id ? updated : item));
          this.successMessage = updated.ativo
            ? `Fonte "${updated.fonte}" ativada no planejamento.`
            : `Fonte "${updated.fonte}" marcada como inativa.`;
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 401) {
            void this.router.navigate(['/login']);
            return;
          }
          this.sessionError = 'Não foi possível atualizar o status da fonte de renda. Tente novamente.';
        },
      });
  }

  requestDeleteIncome(income: Income): void {
    this.deletingIncome = income;
    this.incomeDeleteError = '';
    this.successMessage = '';
    this.incomeDeleteOpen.set(true);
  }

  deleteIncome(): void {
    if (!this.deletingIncome || this.isDeletingIncome) return;
    this.isDeletingIncome = true;
    this.incomeDeleteError = '';
    this.planningService
      .deleteIncome(this.deletingIncome.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isDeletingIncome = false;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.incomeDeleteOpen.set(false);
          this.successMessage = 'Fonte de renda excluída com sucesso.';
          this.loadPlanning();
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 401) {
            void this.router.navigate(['/login']);
            return;
          }
          this.incomeDeleteError =
            error.status === 0
              ? 'Não foi possível confirmar a exclusão. Confira sua conexão e tente novamente.'
              : (error.error as ApiError)?.mensagem ?? 'Não foi possível excluir a fonte de renda. Tente novamente.';
        },
      });
  }

  openExpenseEditor(expense?: RecurringExpense): void {
    this.editingExpense = expense;
    this.successMessage = '';
    this.expenseForm.reset({
      descricao: expense?.descricao ?? '',
      valor: expense ? expense.valor.toFixed(2).replace('.', ',') : '',
      diaVencimento: expense ? String(expense.diaVencimento) : '10',
      ativo: expense?.ativo ?? true,
    });
    this.expenseSubmitted = false;
    this.expenseSaveError = '';
    this.expenseEditorOpen.set(true);
  }

  closeExpenseEditor(): void {
    if (this.isSavingExpense) return;
    this.expenseEditorOpen.set(false);
    this.expenseSaveError = '';
  }

  saveExpense(): void {
    if (this.isSavingExpense) return;
    this.expenseSubmitted = true;
    this.expenseSaveError = '';
    if (this.expenseForm.invalid) {
      this.expenseForm.markAllAsTouched();
      this.expenseFormElement()?.nativeElement.querySelector<HTMLElement>('input.ng-invalid')?.focus();
      return;
    }

    const value = this.expenseForm.getRawValue();
    const amount = parseAmount(value.valor);
    const dueDayRaw = String(value.diaVencimento ?? '').trim();
    const diaVencimento = dueDayRaw === '' ? NaN : Number(dueDayRaw);
    if (amount === null || !Number.isInteger(diaVencimento) || diaVencimento < 1 || diaVencimento > 31) return;

    this.isSavingExpense = true;
    const request = {
      descricao: value.descricao.trim(),
      valor: amount,
      diaVencimento,
      ativo: value.ativo,
    };

    const action$ = this.editingExpense
      ? this.planningService.updateRecurringExpense(this.editingExpense.id, request)
      : this.planningService.createRecurringExpense(request);

    action$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isSavingExpense = false;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.isSavingExpense = false;
          this.closeExpenseEditor();
          this.successMessage = this.editingExpense
            ? 'Gasto recorrente atualizado com sucesso.'
            : 'Gasto recorrente cadastrado com sucesso.';
          this.loadPlanning();
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 401) {
            void this.router.navigate(['/login']);
            return;
          }
          const apiError = error.error as ApiError | undefined;
          this.expenseSaveError =
            error.status === 0
              ? 'Não foi possível confirmar o registro. Confira sua conexão e tente novamente.'
              : Object.values(apiError?.campos ?? {})[0] ??
                apiError?.mensagem ??
                'Não foi possível salvar o gasto recorrente. Tente novamente.';
        },
      });
  }

  toggleExpenseActive(expense: RecurringExpense): void {
    if (this.togglingExpenseId) return;
    this.togglingExpenseId = expense.id;
    this.successMessage = '';
    this.planningService
      .updateRecurringExpense(expense.id, {
        descricao: expense.descricao,
        valor: expense.valor,
        diaVencimento: expense.diaVencimento,
        ativo: !expense.ativo,
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.togglingExpenseId = '';
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: (updated) => {
          this.recurringExpenses = this.recurringExpenses.map((item) => (item.id === updated.id ? updated : item));
          this.successMessage = updated.ativo
            ? `Gasto "${updated.descricao}" ativado no planejamento.`
            : `Gasto "${updated.descricao}" marcado como inativo.`;
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 401) {
            void this.router.navigate(['/login']);
            return;
          }
          this.sessionError = 'Não foi possível atualizar o status do gasto recorrente. Tente novamente.';
        },
      });
  }

  requestDeleteExpense(expense: RecurringExpense): void {
    this.deletingExpense = expense;
    this.expenseDeleteError = '';
    this.successMessage = '';
    this.expenseDeleteOpen.set(true);
  }

  deleteExpense(): void {
    if (!this.deletingExpense || this.isDeletingExpense) return;
    this.isDeletingExpense = true;
    this.expenseDeleteError = '';
    this.planningService
      .deleteRecurringExpense(this.deletingExpense.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isDeletingExpense = false;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.expenseDeleteOpen.set(false);
          this.successMessage = 'Gasto recorrente excluído com sucesso.';
          this.loadPlanning();
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 401) {
            void this.router.navigate(['/login']);
            return;
          }
          this.expenseDeleteError =
            error.status === 0
              ? 'Não foi possível confirmar a exclusão. Confira sua conexão e tente novamente.'
              : (error.error as ApiError)?.mensagem ?? 'Não foi possível excluir o gasto recorrente. Tente novamente.';
        },
      });
  }

  logout(): void {
    this.authService.logout().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => void this.router.navigate(['/login']),
      error: () => {
        this.sessionError = 'Não conseguimos encerrar sua sessão. Tente novamente.';
        this.changeDetector.markForCheck();
      },
    });
  }
}
