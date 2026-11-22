import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { CreditCard } from '../../core/credit-cards/credit-card.models';
import { CreditCardService } from '../../core/credit-cards/credit-card.service';
import { CreditCardsComponent, parseNonNegativeAmount } from './credit-cards.component';

describe('CreditCardsComponent', () => {
  const sampleCards: CreditCard[] = [
    {
      id: 'card-1',
      nomeCartao: 'Cartão Inter',
      limite: 1200.5,
      diaFechamento: 31,
      diaVencimento: 10,
    },
    {
      id: 'card-2',
      nomeCartao: 'Cartão Nubank',
      limite: 3500.75,
      diaFechamento: 8,
      diaVencimento: 15,
    },
    {
      id: 'card-3',
      nomeCartao: 'Cartão Reserva',
      limite: 0,
      diaFechamento: 20,
      diaVencimento: 28,
    },
  ];

  function stubDialogs(page: HTMLElement): void {
    page.querySelectorAll('dialog').forEach((dialog) => {
      Object.defineProperty(dialog, 'showModal', { configurable: true, value: () => dialog.setAttribute('open', '') });
      Object.defineProperty(dialog, 'close', { configurable: true, value: () => dialog.removeAttribute('open') });
    });
  }

  afterEach(() => {
    vi.useRealTimers();
  });

  it('converte valores monetários não negativos incluindo 0,00 com parseNonNegativeAmount', () => {
    expect(parseNonNegativeAmount('0')).toBe(0);
    expect(parseNonNegativeAmount('0,00')).toBe(0);
    expect(parseNonNegativeAmount('0.00')).toBe(0);
    expect(parseNonNegativeAmount('1.234,56')).toBe(1234.56);
    expect(parseNonNegativeAmount('5000')).toBe(5000);
    expect(parseNonNegativeAmount('-10,00')).toBeNull();
    expect(parseNonNegativeAmount('')).toBeNull();
    expect(parseNonNegativeAmount('abc')).toBeNull();
  });

  it('calcula KPIs em centavos inteiros, melhor dia de compra e próximo vencimento', async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 10, 12, 12, 0, 0));

    await TestBed.configureTestingModule({
      imports: [CreditCardsComponent],
      providers: [
        provideRouter([]),
        {
          provide: CreditCardService,
          useValue: {
            listCreditCards: () => of(sampleCards),
          },
        },
        { provide: AuthService, useValue: { logout: () => of(void 0) } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(CreditCardsComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const page = fixture.nativeElement as HTMLElement;

    // 120050 + 350075 + 0 = 470125 centavos -> 4701.25
    expect(component.totalLimitCents).toBe(470125);
    expect(component.totalLimitAmount).toBe(4701.25);
    expect(component.cardsCount).toBe(3);
    expect(component.nextDueCard?.id).toBe('card-2');
    expect(component.nextDueLabel).toBe('Cartão Nubank · vence dia 15');
    expect(component.bestPurchaseDay(sampleCards[0])).toBe(1);
    expect(component.bestPurchaseDay(sampleCards[1])).toBe(9);

    expect(page.textContent).toContain('R$\u00a04.701,25');
    expect(page.textContent).toContain('3 cartões cadastrados');
    expect(page.textContent).toContain('Cartão Nubank · vence dia 15');
    expect(page.textContent).toContain('Fecha dia 31');
    expect(page.textContent).toContain('Melhor dia: 1');
    expect(page.textContent).toContain('Vence dia 10');
    expect(page.textContent).toContain('R$\u00a00,00');
  });

  it('exibe estado vazio claro quando não há cartões cadastrados', async () => {
    await TestBed.configureTestingModule({
      imports: [CreditCardsComponent],
      providers: [
        provideRouter([]),
        {
          provide: CreditCardService,
          useValue: {
            listCreditCards: () => of([]),
          },
        },
        { provide: AuthService, useValue: { logout: () => of(void 0) } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(CreditCardsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;

    expect(fixture.componentInstance.nextDueCard).toBeNull();
    expect(fixture.componentInstance.nextDueLabel).toBe('Sem vencimentos previstos');
    expect(page.textContent).toContain('0 cartões cadastrados');
    expect(page.textContent).toContain('Nenhum cartão de crédito cadastrado');
  });

  it('salva cartão com limite 0,00 e inputs numéricos DOM via NumberValueAccessor e permite editar', async () => {
    const createCreditCard = vi.fn((req) => of({ id: 'card-new', ...req }));
    const updateCreditCard = vi.fn((id, req) => of({ id, ...req }));
    const listCreditCards = vi.fn(() => of(sampleCards));

    await TestBed.configureTestingModule({
      imports: [CreditCardsComponent],
      providers: [
        provideRouter([]),
        {
          provide: CreditCardService,
          useValue: { listCreditCards, createCreditCard, updateCreditCard },
        },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(CreditCardsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    stubDialogs(page);

    fixture.componentInstance.openCardEditor();
    fixture.detectChanges();
    fixture.componentInstance.saveCard();
    expect(createCreditCard).not.toHaveBeenCalled();

    const nameInput = page.querySelector<HTMLInputElement>('#card-name')!;
    const limitInput = page.querySelector<HTMLInputElement>('#card-limit')!;
    const closingInput = page.querySelector<HTMLInputElement>('#card-closing-day')!;
    const dueInput = page.querySelector<HTMLInputElement>('#card-due-day')!;
    const formElement = page.querySelector<HTMLFormElement>('#card-form')!;

    const setDomInput = (input: HTMLInputElement, val: string) => {
      input.value = val;
      input.dispatchEvent(new Event('input', { bubbles: true }));
    };

    // Dia inválido (32) bloqueia envio
    setDomInput(nameInput, 'Cartão C6');
    setDomInput(limitInput, '0,00');
    setDomInput(closingInput, '32');
    setDomInput(dueInput, '10');
    fixture.detectChanges();
    formElement.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(fixture.componentInstance.cardForm.controls.diaFechamento.invalid).toBe(true);
    expect(createCreditCard).not.toHaveBeenCalled();

    // Corrige via eventos DOM reais (NumberValueAccessor grava number no FormControl)
    setDomInput(closingInput, '05');
    setDomInput(dueInput, '12');
    fixture.detectChanges();

    expect(typeof fixture.componentInstance.cardForm.controls.diaFechamento.value).toBe('number');
    expect(typeof fixture.componentInstance.cardForm.controls.diaVencimento.value).toBe('number');

    formElement.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    await fixture.whenStable();

    expect(createCreditCard).toHaveBeenCalledWith({
      nomeCartao: 'Cartão C6',
      limite: 0,
      diaFechamento: 5,
      diaVencimento: 12,
    });
    expect(fixture.componentInstance.cardEditorOpen()).toBe(false);
    expect(page.textContent).toContain('Cartão de crédito cadastrado com sucesso.');

    // Edição de cartão existente
    fixture.componentInstance.openCardEditor(sampleCards[2]);
    fixture.detectChanges();
    expect(fixture.componentInstance.cardForm.controls.limite.value).toBe('0,00');

    setDomInput(limitInput, '2.500,00');
    setDomInput(closingInput, '22');
    fixture.detectChanges();
    formElement.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    await fixture.whenStable();

    expect(updateCreditCard).toHaveBeenCalledWith('card-3', {
      nomeCartao: 'Cartão Reserva',
      limite: 2500,
      diaFechamento: 22,
      diaVencimento: 28,
    });
  });

  it('exclui cartão de crédito após confirmação no modal', async () => {
    const deleteCreditCard = vi.fn(() => of(void 0));
    const listCreditCards = vi.fn(() => of(sampleCards));

    await TestBed.configureTestingModule({
      imports: [CreditCardsComponent],
      providers: [
        provideRouter([]),
        {
          provide: CreditCardService,
          useValue: { listCreditCards, deleteCreditCard },
        },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(CreditCardsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    stubDialogs(page);

    fixture.componentInstance.requestDeleteCard(sampleCards[1]);
    fixture.detectChanges();
    expect(fixture.componentInstance.cardDeleteOpen()).toBe(true);
    expect(page.textContent).toContain('Excluir cartão de crédito?');

    fixture.componentInstance.deleteCard();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(deleteCreditCard).toHaveBeenCalledWith('card-2');
    expect(fixture.componentInstance.cardDeleteOpen()).toBe(false);
    expect(page.textContent).toContain('Cartão de crédito excluído com sucesso.');
  });

  it('trata erros HTTP 400, 401 e 0 na listagem, criação e exclusão', async () => {
    const listCreditCards = vi
      .fn()
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 0 })))
      .mockReturnValue(of(sampleCards));
    const createCreditCard = vi.fn(() =>
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { campos: { nomeCartao: 'Nome do cartão já cadastrado.' } },
          }),
      ),
    );
    const deleteCreditCard = vi.fn(() => throwError(() => new HttpErrorResponse({ status: 401 })));

    await TestBed.configureTestingModule({
      imports: [CreditCardsComponent],
      providers: [
        provideRouter([]),
        {
          provide: CreditCardService,
          useValue: { listCreditCards, createCreditCard, deleteCreditCard },
        },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();

    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const fixture = TestBed.createComponent(CreditCardsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    stubDialogs(page);

    expect(page.textContent).toContain(
      'Não conseguimos carregar seus cartões de crédito. Confira sua conexão e tente novamente.',
    );

    fixture.componentInstance.loadCreditCards();
    fixture.detectChanges();
    expect(fixture.componentInstance.creditCards).toHaveLength(3);

    fixture.componentInstance.openCardEditor();
    fixture.componentInstance.cardForm.setValue({
      nomeCartao: 'Cartão Nubank',
      limite: '1.000,00',
      diaFechamento: '8',
      diaVencimento: '15',
    });
    fixture.componentInstance.saveCard();
    await fixture.whenStable();
    expect(fixture.componentInstance.cardSaveError).toBe('Nome do cartão já cadastrado.');

    fixture.componentInstance.requestDeleteCard(sampleCards[0]);
    fixture.componentInstance.deleteCard();
    await fixture.whenStable();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });
});
