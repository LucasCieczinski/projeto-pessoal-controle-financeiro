import { HttpErrorResponse } from '@angular/common/http';
import { afterNextRender, ChangeDetectorRef, Component, DestroyRef, ElementRef, inject, Injector, OnInit, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { Movement, MovementType } from '../../core/movements/movement.models';
import { MovementService } from '../../core/movements/movement.service';
import { PrivateHeaderComponent } from '../../shared/private-header/private-header.component';
import { PeriodNavigationComponent } from '../../shared/period-navigation/period-navigation.component';
import { ModalComponent } from '../../shared/modal/modal.component';

type MovementFilter = 'TODAS' | MovementType;
interface MovementGroup {
  date: string;
  label: string;
  items: Movement[];
}

const dateInLocalTime = (date: Date): string =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

export function parseAmount(value: string): number | null {
  const input = value.trim().replace(/\s/g, '');
  const isGrouped = /^\d{1,3}(?:\.\d{3})+(?:,\d{1,2})?$/.test(input);
  const isSimple = /^\d+(?:[,.]\d{1,2})?$/.test(input);
  if (!isGrouped && !isSimple) {
    return null;
  }

  const normalized = isGrouped ? input.replace(/\./g, '').replace(',', '.') : input.replace(',', '.');
  const [integer, decimal = ''] = normalized.split('.');
  const cents = Number(integer) * 100 + Number(decimal.padEnd(2, '0'));
  return Number.isSafeInteger(cents) && cents > 0 && cents <= 999_999_999_999 ? cents / 100 : null;
}

const amountValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  parseAmount(String(control.value ?? '')) === null ? { amount: true } : null;

const pastOrPresentValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = String(control.value ?? '');
  const date = new Date(`${value}T12:00:00`);
  return /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(date.getTime())
    && dateInLocalTime(date) === value && value <= dateInLocalTime(new Date())
    ? null : { date: true };
};

@Component({
  selector: 'app-movements',
  imports: [ReactiveFormsModule, RouterLink, PrivateHeaderComponent, PeriodNavigationComponent, ModalComponent],
  templateUrl: './movements.component.html',
  styleUrl: './movements.component.scss',
})
export class MovementsComponent implements OnInit {
  readonly editorOpen = signal(false);
  readonly deleteOpen = signal(false);
  editingMovement?: Movement;
  deletingMovement?: Movement;
  isDeleting = false;
  deleteError = '';
  private readonly formElement = viewChild<ElementRef<HTMLFormElement>>('movementForm');
  private readonly newMovementButton = viewChild<ElementRef<HTMLButtonElement>>('newMovementButton');
  private readonly injector = inject(Injector);

  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly movementService = inject(MovementService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private loadSequence = 0;

  readonly today = dateInLocalTime(new Date());
  readonly form = this.formBuilder.group({
    tipo: this.formBuilder.control<MovementType>('SAIDA', Validators.required),
    descricao: this.formBuilder.control('', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)]),
    valor: this.formBuilder.control('', [Validators.required, amountValidator]),
    categoria: this.formBuilder.control('', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(50)]),
    dataMovimentacao: this.formBuilder.control(this.today, [Validators.required, pastOrPresentValidator]),
  });

  year = new Date().getFullYear();
  monthIndex = new Date().getMonth();
  filter: MovementFilter = 'TODAS';
  movements: Movement[] = [];
  isLoading = true;
  isSaving = false;
  submitted = false;
  loadError = '';
  saveError = '';
  sessionError = '';
  successMessage = '';

  ngOnInit(): void {
    const month = this.route.snapshot.queryParamMap.get('mes');
    if (month && /^[1-9][0-9]{3}-(0[1-9]|1[0-2])$/.test(month) && month <= this.monthKey) {
      const [year, number] = month.split('-').map(Number);
      this.year = year;
      this.monthIndex = number - 1;
    }
    this.loadMonth();
    if (this.route.snapshot.queryParamMap.get('novo') === '1') {
      this.openEditor();
      void this.router.navigate([], { relativeTo: this.route, queryParams: { novo: null }, queryParamsHandling: 'merge', replaceUrl: true });
    }
  }

  get monthKey(): string {
    return `${this.year}-${String(this.monthIndex + 1).padStart(2, '0')}`;
  }

  get monthLabel(): string {
    return new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })
      .format(new Date(this.year, this.monthIndex, 1));
  }

  get monthHeading(): string {
    return this.monthLabel.charAt(0).toUpperCase() + this.monthLabel.slice(1);
  }

  get isCurrentMonth(): boolean {
    const today = new Date();
    return this.year === today.getFullYear() && this.monthIndex === today.getMonth();
  }

  get visibleMovements(): Movement[] {
    return this.filter === 'TODAS' ? this.movements : this.movements.filter((item) => item.tipo === this.filter);
  }

  get groups(): MovementGroup[] {
    const groups: MovementGroup[] = [];
    for (const movement of this.visibleMovements) {
      const last = groups.at(-1);
      if (last?.date === movement.dataMovimentacao) {
        last.items.push(movement);
      } else {
        groups.push({
          date: movement.dataMovimentacao,
          label: new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'long', weekday: 'long' })
            .format(new Date(`${movement.dataMovimentacao}T12:00:00`)),
          items: [movement],
        });
      }
    }
    return groups;
  }

  get categorySuggestions(): string[] {
    return this.form.controls.tipo.value === 'ENTRADA'
      ? ['Salário', 'Freelance', 'Reembolso', 'Presente', 'Outros']
      : ['Alimentação', 'Moradia', 'Transporte', 'Saúde', 'Lazer', 'Educação', 'Outros'];
  }

  countFor(filter: MovementFilter): number {
    return filter === 'TODAS' ? this.movements.length : this.movements.filter((item) => item.tipo === filter).length;
  }

  totalFor(type: MovementType): number {
    return this.movements.filter((item) => item.tipo === type)
      .reduce((cents, item) => cents + Math.round(item.valor * 100), 0) / 100;
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  }

  formatAmountField(): void {
    const amount = parseAmount(this.form.controls.valor.value);
    if (amount === null) return;
    this.form.controls.valor.setValue(new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount));
  }

  fieldInvalid(name: 'descricao' | 'valor' | 'categoria' | 'dataMovimentacao'): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.touched || this.submitted);
  }

  loadMonth(): void {
    const sequence = ++this.loadSequence;
    this.isLoading = true;
    this.loadError = '';
    this.movementService.forMonth(this.monthKey).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (movements) => {
        if (sequence !== this.loadSequence) return;
        this.movements = movements;
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
        this.loadError = error.status === 0
          ? 'Não conseguimos carregar seus registros. Confira sua conexão e tente novamente.'
          : 'Não foi possível carregar seus registros agora. Tente novamente.';
      },
    });
  }

  changeMonth(offset: number): void {
    if (offset > 0 && this.isCurrentMonth) return;
    const next = new Date(this.year, this.monthIndex + offset, 1);
    this.year = next.getFullYear();
    this.monthIndex = next.getMonth();
    this.filter = 'TODAS';
    this.successMessage = '';
    this.loadMonth();
  }

  goToCurrentMonth(): void {
    const today = new Date();
    this.year = today.getFullYear();
    this.monthIndex = today.getMonth();
    this.filter = 'TODAS';
    this.loadMonth();
  }

  openEditor(movement?: Movement): void {
    this.editingMovement = movement;
    this.successMessage = '';
    this.form.reset({
      tipo: movement?.tipo ?? 'SAIDA',
      descricao: movement?.descricao ?? '',
      valor: movement ? movement.valor.toFixed(2).replace('.', ',') : '',
      categoria: movement?.categoria ?? '',
      dataMovimentacao: movement?.dataMovimentacao ?? (this.isCurrentMonth ? this.today : ''),
    });
    this.submitted = false;
    this.saveError = '';
    this.editorOpen.set(true);
  }

  closeEditor(): void {
    if (this.isSaving) return;
    this.editorOpen.set(false);
    this.saveError = '';
  }

  save(): void {
    if (this.isSaving) return;
    this.submitted = true;
    this.saveError = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.formElement()?.nativeElement.querySelector<HTMLElement>('input.ng-invalid')?.focus();
      return;
    }
    const value = this.form.getRawValue();
    const amount = parseAmount(value.valor);
    if (amount === null) return;

    this.isSaving = true;
    const request = {
      tipo: value.tipo,
      descricao: value.descricao.trim(),
      valor: amount,
      categoria: value.categoria.trim(),
      dataMovimentacao: value.dataMovimentacao,
    };
    const saving = this.editingMovement
      ? this.movementService.update(this.editingMovement.id, request)
      : this.movementService.create(request);
    saving.pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.isSaving = false;
        this.changeDetector.markForCheck();
      }),
    ).subscribe({
      next: (movement) => {
        this.isSaving = false;
        this.closeEditor();
        const [year, month] = movement.dataMovimentacao.split('-').map(Number);
        this.year = year;
        this.monthIndex = month - 1;
        this.filter = 'TODAS';
        this.successMessage = this.editingMovement ? 'Movimentação atualizada com sucesso.' : 'Movimentação salva com sucesso.';
        this.loadMonth();
        afterNextRender(() => this.newMovementButton()?.nativeElement.focus(), { injector: this.injector });
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 401) {
          void this.router.navigate(['/login']);
          return;
        }
        const apiError = error.error as ApiError | undefined;
        this.saveError = error.status === 0
          ? 'Não foi possível confirmar o registro. Confira sua conexão e consulte a lista antes de tentar novamente.'
          : Object.values(apiError?.campos ?? {})[0] ?? apiError?.mensagem ?? 'Não foi possível salvar. Tente novamente.';
      },
    });
  }

  requestDelete(movement: Movement): void {
    this.deletingMovement = movement;
    this.deleteError = '';
    this.successMessage = '';
    this.deleteOpen.set(true);
  }

  deleteMovement(): void {
    if (!this.deletingMovement || this.isDeleting) return;
    this.isDeleting = true;
    this.deleteError = '';
    this.movementService.delete(this.deletingMovement.id).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => { this.isDeleting = false; this.changeDetector.markForCheck(); }),
    ).subscribe({
      next: () => {
        this.deleteOpen.set(false);
        this.successMessage = 'Movimentação excluída. Os totais foram atualizados.';
        this.loadMonth();
        // A linha excluída deixa de existir; devolve o foco a uma ação estável da página.
        afterNextRender(() => this.newMovementButton()?.nativeElement.focus(), { injector: this.injector });
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 401) { void this.router.navigate(['/login']); return; }
        this.deleteError = error.status === 0
          ? 'Não foi possível confirmar a exclusão. Confira sua conexão e atualize a lista antes de tentar novamente.'
          : (error.error as ApiError)?.mensagem ?? 'Não foi possível excluir. Tente novamente.';
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
