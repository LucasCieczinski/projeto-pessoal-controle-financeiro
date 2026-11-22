import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, DestroyRef, ElementRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { CreditCard } from '../../core/credit-cards/credit-card.models';
import { CreditCardService } from '../../core/credit-cards/credit-card.service';
import { ModalComponent } from '../../shared/modal/modal.component';
import { PrivateHeaderComponent } from '../../shared/private-header/private-header.component';

export function parseNonNegativeAmount(value: string): number | null {
  const input = value.trim().replace(/\s/g, '');
  const isGrouped = /^\d{1,3}(?:\.\d{3})+(?:,\d{1,2})?$/.test(input);
  const isSimple = /^\d+(?:[,.]\d{1,2})?$/.test(input);
  if (!isGrouped && !isSimple) {
    return null;
  }

  const normalized = isGrouped ? input.replace(/\./g, '').replace(',', '.') : input.replace(',', '.');
  const [integer, decimal = ''] = normalized.split('.');
  const cents = Number(integer) * 100 + Number(decimal.padEnd(2, '0'));
  return Number.isSafeInteger(cents) && cents >= 0 && cents <= 999_999_999_999 ? cents / 100 : null;
}

const nonNegativeAmountValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  parseNonNegativeAmount(String(control.value ?? '')) === null ? { amount: true } : null;

const monthDayValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const raw = String(control.value ?? '').trim();
  if (!/^\d{1,2}$/.test(raw)) return { monthDay: true };
  const day = Number(raw);
  return Number.isInteger(day) && day >= 1 && day <= 31 ? null : { monthDay: true };
};

@Component({
  selector: 'app-credit-cards',
  imports: [ReactiveFormsModule, RouterLink, PrivateHeaderComponent, ModalComponent],
  templateUrl: './credit-cards.component.html',
  styleUrl: './credit-cards.component.scss',
})
export class CreditCardsComponent implements OnInit {
  readonly cardEditorOpen = signal(false);
  readonly cardDeleteOpen = signal(false);

  editingCard?: CreditCard;
  deletingCard?: CreditCard;

  private readonly cardFormElement = viewChild<ElementRef<HTMLFormElement>>('cardFormElement');

  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly creditCardService = inject(CreditCardService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private loadSequence = 0;

  readonly cardForm = this.formBuilder.group({
    nomeCartao: this.formBuilder.control('', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(50)]),
    limite: this.formBuilder.control('', [Validators.required, nonNegativeAmountValidator]),
    diaFechamento: this.formBuilder.control<string | number | null>('', [Validators.required, monthDayValidator]),
    diaVencimento: this.formBuilder.control<string | number | null>('', [Validators.required, monthDayValidator]),
  });

  creditCards: CreditCard[] = [];

  isLoading = true;
  isSavingCard = false;
  isDeletingCard = false;

  cardSubmitted = false;

  loadError = '';
  cardSaveError = '';
  cardDeleteError = '';
  sessionError = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadCreditCards();
  }

  get totalLimitCents(): number {
    return this.creditCards.reduce((cents, card) => cents + Math.round(card.limite * 100), 0);
  }

  get totalLimitAmount(): number {
    return this.totalLimitCents / 100;
  }

  get cardsCount(): number {
    return this.creditCards.length;
  }

  get averageLimitAmount(): number {
    if (this.creditCards.length === 0) return 0;
    return Math.round(this.totalLimitCents / this.creditCards.length) / 100;
  }

  get nextDueCard(): CreditCard | null {
    if (this.creditCards.length === 0) return null;
    const now = new Date();
    return [...this.creditCards].sort(
      (a, b) =>
        this.daysUntilDay(a.diaVencimento, now) - this.daysUntilDay(b.diaVencimento, now) ||
        a.nomeCartao.localeCompare(b.nomeCartao, 'pt-BR'),
    )[0];
  }

  get nextClosingCard(): CreditCard | null {
    if (this.creditCards.length === 0) return null;
    const now = new Date();
    return [...this.creditCards].sort(
      (a, b) =>
        this.daysUntilDay(a.diaFechamento, now) - this.daysUntilDay(b.diaFechamento, now) ||
        a.nomeCartao.localeCompare(b.nomeCartao, 'pt-BR'),
    )[0];
  }

  get nextDueLabel(): string {
    const card = this.nextDueCard;
    if (!card) return 'Sem vencimentos previstos';
    return `${card.nomeCartao} · vence dia ${card.diaVencimento}`;
  }

  get nextDueHint(): string {
    const dueCard = this.nextDueCard;
    if (!dueCard) return 'Cadastre um cartão para acompanhar fechamento e vencimento';
    const days = this.daysUntilDay(dueCard.diaVencimento);
    const dueTiming = days === 0 ? 'Vence hoje' : days === 1 ? 'Vence amanhã' : `Em ${days} dias`;
    const closingCard = this.nextClosingCard;
    if (closingCard && closingCard.id !== dueCard.id) {
      return `${dueTiming} · próximo fechamento: ${closingCard.nomeCartao} (dia ${closingCard.diaFechamento})`;
    }
    return `${dueTiming} · fecha dia ${dueCard.diaFechamento} (melhor dia: ${this.bestPurchaseDay(dueCard)})`;
  }

  get closingDayPreview(): number | null {
    const raw = String(this.cardForm.controls.diaFechamento.value ?? '').trim();
    if (!/^\d{1,2}$/.test(raw)) return null;
    const day = Number(raw);
    if (!Number.isInteger(day) || day < 1 || day > 31) return null;
    return day === 31 ? 1 : day + 1;
  }

  bestPurchaseDay(card: CreditCard): number {
    return card.diaFechamento === 31 ? 1 : card.diaFechamento + 1;
  }

  daysUntilDay(day: number, referenceDate = new Date()): number {
    const currentDay = referenceDate.getDate();
    if (day >= currentDay) {
      return day - currentDay;
    }
    const daysInCurrentMonth = new Date(
      referenceDate.getFullYear(),
      referenceDate.getMonth() + 1,
      0,
    ).getDate();
    return Math.max(0, daysInCurrentMonth - currentDay) + day;
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  }

  formatCardLimitField(): void {
    const amount = parseNonNegativeAmount(String(this.cardForm.controls.limite.value ?? ''));
    if (amount === null) return;
    this.cardForm.controls.limite.setValue(
      new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount),
    );
  }

  cardFieldInvalid(name: 'nomeCartao' | 'limite' | 'diaFechamento' | 'diaVencimento'): boolean {
    const control = this.cardForm.controls[name];
    return control.invalid && (control.touched || this.cardSubmitted);
  }

  loadCreditCards(): void {
    const sequence = ++this.loadSequence;
    this.isLoading = true;
    this.loadError = '';
    this.creditCardService
      .listCreditCards()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cards) => {
          if (sequence !== this.loadSequence) return;
          this.creditCards = cards;
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
              ? 'Não conseguimos carregar seus cartões de crédito. Confira sua conexão e tente novamente.'
              : 'Não foi possível carregar seus cartões de crédito agora. Tente novamente.';
        },
      });
  }

  openCardEditor(card?: CreditCard): void {
    this.editingCard = card;
    this.successMessage = '';
    this.cardForm.reset({
      nomeCartao: card?.nomeCartao ?? '',
      limite: card ? card.limite.toFixed(2).replace('.', ',') : '',
      diaFechamento: card ? String(card.diaFechamento) : '',
      diaVencimento: card ? String(card.diaVencimento) : '',
    });
    this.cardSubmitted = false;
    this.cardSaveError = '';
    this.cardEditorOpen.set(true);
  }

  closeCardEditor(): void {
    if (this.isSavingCard) return;
    this.cardEditorOpen.set(false);
    this.cardSaveError = '';
  }

  saveCard(): void {
    if (this.isSavingCard) return;
    this.cardSubmitted = true;
    this.cardSaveError = '';
    if (this.cardForm.invalid) {
      this.cardForm.markAllAsTouched();
      this.cardFormElement()?.nativeElement.querySelector<HTMLElement>('input.ng-invalid')?.focus();
      return;
    }

    const value = this.cardForm.getRawValue();
    const limite = parseNonNegativeAmount(String(value.limite ?? ''));
    if (limite === null) return;

    const closingRaw = String(value.diaFechamento ?? '').trim();
    const dueRaw = String(value.diaVencimento ?? '').trim();
    if (!/^\d{1,2}$/.test(closingRaw) || !/^\d{1,2}$/.test(dueRaw)) return;

    const diaFechamento = Number(closingRaw);
    const diaVencimento = Number(dueRaw);
    if (!Number.isInteger(diaFechamento) || diaFechamento < 1 || diaFechamento > 31) return;
    if (!Number.isInteger(diaVencimento) || diaVencimento < 1 || diaVencimento > 31) return;

    this.isSavingCard = true;
    const request = {
      nomeCartao: String(value.nomeCartao ?? '').trim(),
      limite,
      diaFechamento,
      diaVencimento,
    };

    const action$ = this.editingCard
      ? this.creditCardService.updateCreditCard(this.editingCard.id, request)
      : this.creditCardService.createCreditCard(request);

    action$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isSavingCard = false;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.isSavingCard = false;
          this.closeCardEditor();
          this.successMessage = this.editingCard
            ? 'Cartão de crédito atualizado com sucesso.'
            : 'Cartão de crédito cadastrado com sucesso.';
          this.loadCreditCards();
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 401) {
            void this.router.navigate(['/login']);
            return;
          }
          const apiError = error.error as ApiError | undefined;
          this.cardSaveError =
            error.status === 0
              ? 'Não foi possível confirmar o registro. Confira sua conexão e tente novamente.'
              : Object.values(apiError?.campos ?? {})[0] ??
                apiError?.mensagem ??
                'Não foi possível salvar o cartão de crédito. Tente novamente.';
        },
      });
  }

  requestDeleteCard(card: CreditCard): void {
    this.deletingCard = card;
    this.cardDeleteError = '';
    this.successMessage = '';
    this.cardDeleteOpen.set(true);
  }

  deleteCard(): void {
    if (!this.deletingCard || this.isDeletingCard) return;
    this.isDeletingCard = true;
    this.cardDeleteError = '';
    this.creditCardService
      .deleteCreditCard(this.deletingCard.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isDeletingCard = false;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.cardDeleteOpen.set(false);
          this.successMessage = 'Cartão de crédito excluído com sucesso.';
          this.loadCreditCards();
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 401) {
            void this.router.navigate(['/login']);
            return;
          }
          this.cardDeleteError =
            error.status === 0
              ? 'Não foi possível confirmar a exclusão. Confira sua conexão e tente novamente.'
              : (error.error as ApiError)?.mensagem ?? 'Não foi possível excluir o cartão de crédito. Tente novamente.';
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
