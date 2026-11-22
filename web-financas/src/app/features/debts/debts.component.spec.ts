import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { Debt } from '../../core/debts/debt.models';
import { DebtService } from '../../core/debts/debt.service';
import { DebtsComponent } from './debts.component';

describe('DebtsComponent', () => {
  const sampleDebts: Debt[] = [
    {
      id: 'debt-1',
      credor: 'Financiamento Banco Itaú',
      valorTotal: 4800,
      totalParcelas: 12,
      parcelasPagas: 3,
      status: 'ABERTA',
    },
    {
      id: 'debt-2',
      credor: 'Empréstimo Pessoal',
      valorTotal: 1500.5,
      totalParcelas: 5,
      parcelasPagas: 1,
      status: 'ATRASADA',
    },
    {
      id: 'debt-3',
      credor: 'Acordo Cartão Antigo',
      valorTotal: 900,
      totalParcelas: null,
      parcelasPagas: 1,
      status: 'QUITADA',
    },
  ];

  function stubDialogs(page: HTMLElement): void {
    page.querySelectorAll('dialog').forEach((dialog) => {
      Object.defineProperty(dialog, 'showModal', { configurable: true, value: () => dialog.setAttribute('open', '') });
      Object.defineProperty(dialog, 'close', { configurable: true, value: () => dialog.removeAttribute('open') });
    });
  }

  it('renderiza o resumo de passivos em centavos inteiros, progresso de parcelas e filtra por status', async () => {
    await TestBed.configureTestingModule({
      imports: [DebtsComponent],
      providers: [
        provideRouter([]),
        {
          provide: DebtService,
          useValue: {
            listDebts: () => of(sampleDebts),
          },
        },
        { provide: AuthService, useValue: { logout: () => of(void 0) } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(DebtsComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const page = fixture.nativeElement as HTMLElement;

    expect(component.totalActiveDebtsAmount).toBe(6300.5);
    expect(component.activeDebtsCount).toBe(2);
    expect(component.overdueDebtsCount).toBe(1);
    expect(component.settledDebtsCount).toBe(1);
    expect(component.totalInstallmentsSum).toBe(18);
    expect(component.paidInstallmentsSum).toBe(5);
    expect(component.overallProgressPercent).toBe(28);

    expect(page.textContent).toContain('3 de 12 parcelas pagas (25%)');
    expect(page.textContent).toContain('Pagamento único · Quitada');

    component.setStatusFilter('ATRASADA');
    fixture.detectChanges();
    expect(component.filteredDebts).toHaveLength(1);
    expect(component.filteredDebts[0].credor).toBe('Empréstimo Pessoal');

    component.setStatusFilter('QUITADA');
    fixture.detectChanges();
    expect(component.filteredDebts).toHaveLength(1);
    expect(component.filteredDebts[0].credor).toBe('Acordo Cartão Antigo');

    component.setStatusFilter('TODOS');
    fixture.detectChanges();
    expect(component.filteredDebts).toHaveLength(3);
  });

  it('valida formulário (parcelasPagas <= totalParcelas e valor > 0), cria e edita dívida', async () => {
    const createDebt = vi.fn((req) => of({ id: 'debt-new', ...req }));
    const updateDebt = vi.fn((id, req) => of({ id, ...req }));
    const listDebts = vi.fn(() => of(sampleDebts));

    await TestBed.configureTestingModule({
      imports: [DebtsComponent],
      providers: [
        provideRouter([]),
        {
          provide: DebtService,
          useValue: { listDebts, createDebt, updateDebt },
        },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(DebtsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    stubDialogs(page);

    fixture.componentInstance.openDebtEditor();
    fixture.detectChanges();
    fixture.componentInstance.saveDebt();
    expect(createDebt).not.toHaveBeenCalled();

    // Parcelas pagas maior que totalParcelas deve bloquear envio
    fixture.componentInstance.debtForm.setValue({
      credor: 'Consórcio Auto',
      valorTotal: '12.000,00',
      totalParcelas: '10',
      parcelasPagas: '11',
      status: 'ABERTA',
    });
    fixture.componentInstance.saveDebt();
    expect(fixture.componentInstance.debtForm.hasError('paidExceedsTotal')).toBe(true);
    expect(createDebt).not.toHaveBeenCalled();

    // Corrige parcelasPagas e salva
    fixture.componentInstance.debtForm.controls.parcelasPagas.setValue('2');
    fixture.componentInstance.saveDebt();
    await fixture.whenStable();

    expect(createDebt).toHaveBeenCalledWith({
      credor: 'Consórcio Auto',
      valorTotal: 12000,
      totalParcelas: 10,
      parcelasPagas: 2,
      status: 'ABERTA',
    });
    expect(fixture.componentInstance.debtEditorOpen()).toBe(false);
    expect(page.textContent).toContain('Dívida cadastrada com sucesso.');

    // Edição de dívida existente
    fixture.componentInstance.openDebtEditor(sampleDebts[0]);
    fixture.detectChanges();
    expect(fixture.componentInstance.debtForm.controls.valorTotal.value).toBe('4800,00');
    fixture.componentInstance.debtForm.controls.totalParcelas.setValue('');
    fixture.componentInstance.debtForm.controls.parcelasPagas.setValue('0');
    fixture.componentInstance.saveDebt();
    await fixture.whenStable();

    expect(updateDebt).toHaveBeenCalledWith('debt-1', {
      credor: 'Financiamento Banco Itaú',
      valorTotal: 4800,
      totalParcelas: null,
      parcelasPagas: 0,
      status: 'ABERTA',
    });
  });

  it('executa ação rápida de pagar parcela e exclui dívida com confirmação', async () => {
    const payInstallment = vi.fn((id: string) =>
      of({
        ...sampleDebts[0],
        id,
        parcelasPagas: 4,
        status: 'ABERTA' as const,
      }),
    );
    const deleteDebt = vi.fn(() => of(void 0));

    await TestBed.configureTestingModule({
      imports: [DebtsComponent],
      providers: [
        provideRouter([]),
        {
          provide: DebtService,
          useValue: {
            listDebts: () => of(sampleDebts),
            payInstallment,
            deleteDebt,
          },
        },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(DebtsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    stubDialogs(page);

    fixture.componentInstance.payInstallment(sampleDebts[0]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(payInstallment).toHaveBeenCalledWith('debt-1');
    expect(fixture.componentInstance.debts[0].parcelasPagas).toBe(4);
    expect(page.textContent).toContain('Parcela registrada para "Financiamento Banco Itaú".');

    fixture.componentInstance.requestDeleteDebt(sampleDebts[1]);
    fixture.detectChanges();
    expect(fixture.componentInstance.debtDeleteOpen()).toBe(true);
    fixture.componentInstance.deleteDebt();
    await fixture.whenStable();
    expect(deleteDebt).toHaveBeenCalledWith('debt-2');
    expect(fixture.componentInstance.debtDeleteOpen()).toBe(false);
  });

  it('trata erros HTTP 400, 401 e 0 na listagem, criação e pagamento de parcela', async () => {
    const listDebts = vi
      .fn()
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 0 })))
      .mockReturnValue(of(sampleDebts));
    const createDebt = vi.fn(() =>
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { campos: { parcelasPagas: 'Parcelas pagas inválidas.' } },
          }),
      ),
    );
    const payInstallment = vi.fn(() => throwError(() => new HttpErrorResponse({ status: 401 })));

    await TestBed.configureTestingModule({
      imports: [DebtsComponent],
      providers: [
        provideRouter([]),
        {
          provide: DebtService,
          useValue: { listDebts, createDebt, payInstallment },
        },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();

    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const fixture = TestBed.createComponent(DebtsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    stubDialogs(page);

    // Erro 0 na carga inicial
    expect(page.textContent).toContain('Não conseguimos carregar suas dívidas. Confira sua conexão e tente novamente.');

    // Tenta novamente e carrega com sucesso
    fixture.componentInstance.loadDebts();
    fixture.detectChanges();
    expect(fixture.componentInstance.debts).toHaveLength(3);

    // Erro 400 ao salvar no modal
    fixture.componentInstance.openDebtEditor();
    fixture.componentInstance.debtForm.setValue({
      credor: 'Cartão Loja',
      valorTotal: '500,00',
      totalParcelas: '5',
      parcelasPagas: '1',
      status: 'ABERTA',
    });
    fixture.componentInstance.saveDebt();
    await fixture.whenStable();
    expect(fixture.componentInstance.debtSaveError).toBe('Parcelas pagas inválidas.');

    // Erro 401 ao pagar parcela redireciona para /login
    fixture.componentInstance.payInstallment(sampleDebts[0]);
    await fixture.whenStable();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('salva dívida corretamente quando inputs numéricos do DOM atualizam o formulário via NumberValueAccessor', async () => {
    const createDebt = vi.fn((req) => of({ id: 'debt-dom', ...req }));
    const listDebts = vi.fn(() => of(sampleDebts));

    await TestBed.configureTestingModule({
      imports: [DebtsComponent],
      providers: [
        provideRouter([]),
        {
          provide: DebtService,
          useValue: { listDebts, createDebt },
        },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(DebtsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    stubDialogs(page);

    fixture.componentInstance.openDebtEditor();
    fixture.detectChanges();

    const creditorInput = page.querySelector<HTMLInputElement>('#debt-creditor')!;
    const amountInput = page.querySelector<HTMLInputElement>('#debt-amount')!;
    const totalInput = page.querySelector<HTMLInputElement>('#debt-total-installments')!;
    const paidInput = page.querySelector<HTMLInputElement>('#debt-paid-installments')!;
    const formElement = page.querySelector<HTMLFormElement>('#debt-form')!;

    const setDomInput = (input: HTMLInputElement, val: string) => {
      input.value = val;
      input.dispatchEvent(new Event('input', { bubbles: true }));
    };

    setDomInput(creditorInput, 'teste');
    setDomInput(amountInput, '111,00');
    setDomInput(totalInput, '11');
    setDomInput(paidInput, '02');
    fixture.detectChanges();

    // Confirma que NumberValueAccessor gravou valores numéricos no FormControl
    expect(typeof fixture.componentInstance.debtForm.controls.totalParcelas.value).toBe('number');
    expect(typeof fixture.componentInstance.debtForm.controls.parcelasPagas.value).toBe('number');

    formElement.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    await fixture.whenStable();

    expect(createDebt).toHaveBeenCalledWith({
      credor: 'teste',
      valorTotal: 111,
      totalParcelas: 11,
      parcelasPagas: 2,
      status: 'ABERTA',
    });

    // Abre novamente e limpa o totalParcelas via DOM (NumberValueAccessor grava null)
    fixture.componentInstance.openDebtEditor();
    fixture.detectChanges();

    setDomInput(creditorInput, 'Pagamento à vista');
    setDomInput(amountInput, '250,00');
    setDomInput(totalInput, '');
    setDomInput(paidInput, '0');
    fixture.detectChanges();

    expect(fixture.componentInstance.debtForm.controls.totalParcelas.value).toBeNull();

    formElement.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    await fixture.whenStable();

    expect(createDebt).toHaveBeenCalledWith({
      credor: 'Pagamento à vista',
      valorTotal: 250,
      totalParcelas: null,
      parcelasPagas: 0,
      status: 'ABERTA',
    });
  });
});
