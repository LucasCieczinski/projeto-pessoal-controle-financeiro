import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, DestroyRef, ElementRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { Debt, DebtStatus } from '../../core/debts/debt.models';
import { DebtService } from '../../core/debts/debt.service';
import { ModalComponent } from '../../shared/modal/modal.component';
import { PrivateHeaderComponent } from '../../shared/private-header/private-header.component';
import { parseAmount } from '../movements/movements.component';

export type DebtStatusFilter = 'TODOS' | DebtStatus;

const amountValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  parseAmount(String(control.value ?? '')) === null ? { amount: true } : null;

const totalInstallmentsValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const raw = String(control.value ?? '').trim();
  if (!raw) return null;
  if (!/^\d+$/.test(raw)) return { totalParcelas: true };
  const total = Number(raw);
  return Number.isInteger(total) && total >= 1 ? null : { totalParcelas: true };
};

const paidInstallmentsValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const raw = String(control.value ?? '').trim();
  if (!/^\d+$/.test(raw)) return { parcelasPagas: true };
  const paid = Number(raw);
  return Number.isInteger(paid) && paid >= 0 ? null : { parcelasPagas: true };
};

const installmentConsistencyValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const totalRaw = String(group.get('totalParcelas')?.value ?? '').trim();
  const paidRaw = String(group.get('parcelasPagas')?.value ?? '').trim();
  if (!totalRaw || !/^\d+$/.test(totalRaw) || !/^\d+$/.test(paidRaw)) return null;
  const total = Number(totalRaw);
  const paid = Number(paidRaw);
  if (total >= 1 && paid > total) {
    return { paidExceedsTotal: true };
  }
  return null;
};

@Component({
  selector: 'app-debts',
  imports: [ReactiveFormsModule, RouterLink, PrivateHeaderComponent, ModalComponent],
  templateUrl: './debts.component.html',
  styleUrl: './debts.component.scss',
})
export class DebtsComponent implements OnInit {
  readonly debtEditorOpen = signal(false);
  readonly debtDeleteOpen = signal(false);
  readonly statusFilter = signal<DebtStatusFilter>('TODOS');

  editingDebt?: Debt;
  deletingDebt?: Debt;

  private readonly debtFormElement = viewChild<ElementRef<HTMLFormElement>>('debtFormElement');

  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly debtService = inject(DebtService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private loadSequence = 0;

  readonly debtForm = this.formBuilder.group(
    {
      credor: this.formBuilder.control('', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)]),
      valorTotal: this.formBuilder.control('', [Validators.required, amountValidator]),
      totalParcelas: this.formBuilder.control<string | number | null>('', [totalInstallmentsValidator]),
      parcelasPagas: this.formBuilder.control<string | number | null>('0', [Validators.required, paidInstallmentsValidator]),
      status: this.formBuilder.control<DebtStatus>('ABERTA', [Validators.required]),
    },
    { validators: [installmentConsistencyValidator] },
  );

  debts: Debt[] = [];

  isLoading = true;
  isSavingDebt = false;
  isDeletingDebt = false;
  payingDebtId = '';

  debtSubmitted = false;

  loadError = '';
  debtSaveError = '';
  debtDeleteError = '';
  sessionError = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadDebts();
  }

  get activeDebtsCents(): number {
    return this.debts
      .filter((debt) => debt.status === 'ABERTA' || debt.status === 'ATRASADA')
      .reduce((cents, debt) => cents + Math.round(debt.valorTotal * 100), 0);
  }

  get totalActiveDebtsAmount(): number {
    return this.activeDebtsCents / 100;
  }

  get activeDebtsCount(): number {
    return this.debts.filter((debt) => debt.status === 'ABERTA' || debt.status === 'ATRASADA').length;
  }

  get overdueDebtsCount(): number {
    return this.debts.filter((debt) => debt.status === 'ATRASADA').length;
  }

  get settledDebtsCount(): number {
    return this.debts.filter((debt) => debt.status === 'QUITADA').length;
  }

  get totalInstallmentsSum(): number {
    return this.debts.reduce((sum, debt) => sum + (debt.totalParcelas ?? 1), 0);
  }

  get paidInstallmentsSum(): number {
    return this.debts.reduce((sum, debt) => {
      if (debt.totalParcelas !== null) {
        return sum + Math.min(debt.parcelasPagas, debt.totalParcelas);
      }
      return sum + (debt.status === 'QUITADA' || debt.parcelasPagas > 0 ? 1 : 0);
    }, 0);
  }

  get overallProgressPercent(): number {
    if (this.debts.length === 0 || this.totalInstallmentsSum <= 0) return 0;
    return Math.round((this.paidInstallmentsSum / this.totalInstallmentsSum) * 100);
  }

  get filteredDebts(): Debt[] {
    const filter = this.statusFilter();
    if (filter === 'TODOS') return this.debts;
    return this.debts.filter((debt) => debt.status === filter);
  }

  setStatusFilter(filter: DebtStatusFilter): void {
    this.statusFilter.set(filter);
  }

  statusLabel(status: DebtStatus): string {
    switch (status) {
      case 'ABERTA':
        return 'Aberta';
      case 'ATRASADA':
        return 'Atrasada';
      case 'QUITADA':
        return 'Quitada';
    }
  }

  debtProgressPercent(debt: Debt): number {
    if (debt.status === 'QUITADA') return 100;
    if (debt.totalParcelas === null || debt.totalParcelas <= 0) {
      return debt.parcelasPagas > 0 ? 100 : 0;
    }
    return Math.min(100, Math.round((debt.parcelasPagas / debt.totalParcelas) * 100));
  }

  debtProgressCopy(debt: Debt): string {
    if (debt.totalParcelas === null) {
      return debt.status === 'QUITADA' ? 'Pagamento único · Quitada' : 'Pagamento único';
    }
    const percent = this.debtProgressPercent(debt);
    return `${debt.parcelasPagas} de ${debt.totalParcelas} parcelas pagas (${percent}%)`;
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  }

  formatDebtAmountField(): void {
    const amount = parseAmount(this.debtForm.controls.valorTotal.value);
    if (amount === null) return;
    this.debtForm.controls.valorTotal.setValue(
      new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount),
    );
  }

  debtFieldInvalid(name: 'credor' | 'valorTotal' | 'totalParcelas' | 'parcelasPagas'): boolean {
    const control = this.debtForm.controls[name];
    const groupError = name === 'parcelasPagas' && this.debtForm.hasError('paidExceedsTotal');
    return (control.invalid || groupError) && (control.touched || this.debtSubmitted);
  }

  loadDebts(): void {
    const sequence = ++this.loadSequence;
    this.isLoading = true;
    this.loadError = '';
    this.debtService
      .listDebts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (debts) => {
          if (sequence !== this.loadSequence) return;
          this.debts = debts;
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
              ? 'Não conseguimos carregar suas dívidas. Confira sua conexão e tente novamente.'
              : 'Não foi possível carregar suas dívidas agora. Tente novamente.';
        },
      });
  }

  openDebtEditor(debt?: Debt): void {
    this.editingDebt = debt;
    this.successMessage = '';
    this.debtForm.reset({
      credor: debt?.credor ?? '',
      valorTotal: debt ? debt.valorTotal.toFixed(2).replace('.', ',') : '',
      totalParcelas: debt && debt.totalParcelas !== null ? String(debt.totalParcelas) : '',
      parcelasPagas: debt ? String(debt.parcelasPagas) : '0',
      status: debt?.status ?? 'ABERTA',
    });
    this.debtSubmitted = false;
    this.debtSaveError = '';
    this.debtEditorOpen.set(true);
  }

  closeDebtEditor(): void {
    if (this.isSavingDebt) return;
    this.debtEditorOpen.set(false);
    this.debtSaveError = '';
  }

  saveDebt(): void {
    if (this.isSavingDebt) return;
    this.debtSubmitted = true;
    this.debtSaveError = '';
    if (this.debtForm.invalid) {
      this.debtForm.markAllAsTouched();
      this.debtFormElement()?.nativeElement.querySelector<HTMLElement>('input.ng-invalid, select.ng-invalid')?.focus();
      return;
    }

    const value = this.debtForm.getRawValue();
    const amount = parseAmount(value.valorTotal);
    if (amount === null) return;

    const totalRaw = String(value.totalParcelas ?? '').trim();
    const totalParcelas = totalRaw === '' ? null : Number(totalRaw);
    const paidRaw = String(value.parcelasPagas ?? '').trim();
    if (paidRaw === '') return;
    const parcelasPagas = Number(paidRaw);

    if (totalParcelas !== null && (!Number.isInteger(totalParcelas) || totalParcelas < 1)) return;
    if (!Number.isInteger(parcelasPagas) || parcelasPagas < 0) return;
    if (totalParcelas !== null && parcelasPagas > totalParcelas) return;

    this.isSavingDebt = true;
    const request = {
      credor: value.credor.trim(),
      valorTotal: amount,
      totalParcelas,
      parcelasPagas,
      status: value.status,
    };

    const action$ = this.editingDebt
      ? this.debtService.updateDebt(this.editingDebt.id, request)
      : this.debtService.createDebt(request);

    action$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isSavingDebt = false;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.isSavingDebt = false;
          this.closeDebtEditor();
          this.successMessage = this.editingDebt
            ? 'Dívida atualizada com sucesso.'
            : 'Dívida cadastrada com sucesso.';
          this.loadDebts();
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 401) {
            void this.router.navigate(['/login']);
            return;
          }
          const apiError = error.error as ApiError | undefined;
          this.debtSaveError =
            error.status === 0
              ? 'Não foi possível confirmar o registro. Confira sua conexão e tente novamente.'
              : Object.values(apiError?.campos ?? {})[0] ??
                apiError?.mensagem ??
                'Não foi possível salvar a dívida. Tente novamente.';
        },
      });
  }

  payInstallment(debt: Debt): void {
    if (this.payingDebtId || debt.status === 'QUITADA') return;
    this.payingDebtId = debt.id;
    this.sessionError = '';
    this.successMessage = '';
    this.debtService
      .payInstallment(debt.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.payingDebtId = '';
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: (updated) => {
          this.debts = this.debts.map((item) => (item.id === updated.id ? updated : item));
          this.successMessage =
            updated.status === 'QUITADA'
              ? `Dívida com "${updated.credor}" quitada com sucesso!`
              : `Parcela registrada para "${updated.credor}".`;
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 401) {
            void this.router.navigate(['/login']);
            return;
          }
          const apiError = error.error as ApiError | undefined;
          this.sessionError =
            error.status === 0
              ? 'Não foi possível registrar o pagamento da parcela. Confira sua conexão e tente novamente.'
              : apiError?.mensagem ?? 'Não foi possível registrar o pagamento da parcela. Tente novamente.';
        },
      });
  }

  requestDeleteDebt(debt: Debt): void {
    this.deletingDebt = debt;
    this.debtDeleteError = '';
    this.successMessage = '';
    this.debtDeleteOpen.set(true);
  }

  deleteDebt(): void {
    if (!this.deletingDebt || this.isDeletingDebt) return;
    this.isDeletingDebt = true;
    this.debtDeleteError = '';
    this.debtService
      .deleteDebt(this.deletingDebt.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isDeletingDebt = false;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.debtDeleteOpen.set(false);
          this.successMessage = 'Dívida excluída com sucesso.';
          this.loadDebts();
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 401) {
            void this.router.navigate(['/login']);
            return;
          }
          this.debtDeleteError =
            error.status === 0
              ? 'Não foi possível confirmar a exclusão. Confira sua conexão e tente novamente.'
              : (error.error as ApiError)?.mensagem ?? 'Não foi possível excluir a dívida. Tente novamente.';
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
