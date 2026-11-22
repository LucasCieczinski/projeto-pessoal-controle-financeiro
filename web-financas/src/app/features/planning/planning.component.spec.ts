import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { Income, RecurringExpense } from '../../core/planning/planning.models';
import { PlanningService } from '../../core/planning/planning.service';
import { PlanningComponent } from './planning.component';

describe('PlanningComponent', () => {
  const sampleIncomes: Income[] = [
    { id: 'inc-1', fonte: 'Salário CLT', valorBase: 5000, ativo: true },
    { id: 'inc-2', fonte: 'Consultoria eventual', valorBase: 1500, ativo: false },
  ];

  const sampleExpenses: RecurringExpense[] = [
    { id: 'exp-1', descricao: 'Internet Fibra', valor: 150, diaVencimento: 20, ativo: true },
    { id: 'exp-2', descricao: 'Aluguel', valor: 1850, diaVencimento: 5, ativo: true },
    { id: 'exp-3', descricao: 'Streaming pausado', valor: 49.9, diaVencimento: 15, ativo: false },
  ];

  function stubDialogs(page: HTMLElement): void {
    page.querySelectorAll('dialog').forEach((dialog) => {
      Object.defineProperty(dialog, 'showModal', { configurable: true, value: () => dialog.setAttribute('open', '') });
      Object.defineProperty(dialog, 'close', { configurable: true, value: () => dialog.removeAttribute('open') });
    });
  }

  it('calcula o comprometimento fixo considerando apenas rendas e gastos ativos e ordena por dia de vencimento', async () => {
    await TestBed.configureTestingModule({
      imports: [PlanningComponent],
      providers: [
        provideRouter([]),
        {
          provide: PlanningService,
          useValue: {
            listIncomes: () => of(sampleIncomes),
            listRecurringExpenses: () => of(sampleExpenses),
          },
        },
        { provide: AuthService, useValue: { logout: () => of(void 0) } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(PlanningComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const page = fixture.nativeElement as HTMLElement;

    expect(component.totalActiveIncomes).toBe(5000);
    expect(component.totalActiveExpenses).toBe(2000);
    expect(component.freeMargin).toBe(3000);
    expect(component.commitmentPercent).toBe(40);
    expect(page.textContent).toContain('40% da renda ativa comprometida');

    const sortedDescriptions = component.sortedRecurringExpenses.map((item) => item.descricao);
    expect(sortedDescriptions).toEqual(['Aluguel', 'Streaming pausado', 'Internet Fibra']);
  });

  it('valida, cria e edita fonte de renda com precisão em centavos', async () => {
    const createIncome = vi.fn((req) => of({ id: 'inc-new', ...req }));
    const updateIncome = vi.fn((id, req) => of({ id, ...req }));
    const listIncomes = vi.fn(() => of(sampleIncomes));
    const listRecurringExpenses = vi.fn(() => of([]));

    await TestBed.configureTestingModule({
      imports: [PlanningComponent],
      providers: [
        provideRouter([]),
        {
          provide: PlanningService,
          useValue: { listIncomes, listRecurringExpenses, createIncome, updateIncome },
        },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(PlanningComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    stubDialogs(page);

    fixture.componentInstance.openIncomeEditor();
    fixture.detectChanges();
    fixture.componentInstance.saveIncome();
    expect(createIncome).not.toHaveBeenCalled();

    fixture.componentInstance.incomeForm.setValue({
      fonte: '  Freelance Fixo  ',
      valorBase: '2.500,50',
      ativo: true,
    });
    fixture.componentInstance.saveIncome();
    await fixture.whenStable();

    expect(createIncome).toHaveBeenCalledWith({
      fonte: 'Freelance Fixo',
      valorBase: 2500.5,
      ativo: true,
    });
    expect(fixture.componentInstance.incomeEditorOpen()).toBe(false);
    expect(page.textContent).toContain('Fonte de renda cadastrada com sucesso.');

    fixture.componentInstance.openIncomeEditor(sampleIncomes[0]);
    fixture.detectChanges();
    expect(fixture.componentInstance.incomeForm.controls.valorBase.value).toBe('5000,00');
    fixture.componentInstance.incomeForm.controls.valorBase.setValue('5.200,00');
    fixture.componentInstance.saveIncome();
    await fixture.whenStable();

    expect(updateIncome).toHaveBeenCalledWith('inc-1', {
      fonte: 'Salário CLT',
      valorBase: 5200,
      ativo: true,
    });
  });

  it('valida dia de vencimento (1..31), cria, alterna status e exclui gasto recorrente', async () => {
    const createRecurringExpense = vi.fn((req) => of({ id: 'exp-new', ...req }));
    const updateRecurringExpense = vi.fn((id, req) => of({ id, ...req }));
    const deleteRecurringExpense = vi.fn(() => of(void 0));

    await TestBed.configureTestingModule({
      imports: [PlanningComponent],
      providers: [
        provideRouter([]),
        {
          provide: PlanningService,
          useValue: {
            listIncomes: () => of([]),
            listRecurringExpenses: () => of(sampleExpenses),
            createRecurringExpense,
            updateRecurringExpense,
            deleteRecurringExpense,
          },
        },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(PlanningComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    stubDialogs(page);

    fixture.componentInstance.openExpenseEditor();
    fixture.componentInstance.expenseForm.setValue({
      descricao: 'Condomínio',
      valor: '450,00',
      diaVencimento: '32',
      ativo: true,
    });
    fixture.componentInstance.saveExpense();
    expect(createRecurringExpense).not.toHaveBeenCalled();

    fixture.componentInstance.expenseForm.controls.diaVencimento.setValue('10');
    fixture.componentInstance.saveExpense();
    await fixture.whenStable();

    expect(createRecurringExpense).toHaveBeenCalledWith({
      descricao: 'Condomínio',
      valor: 450,
      diaVencimento: 10,
      ativo: true,
    });

    fixture.componentInstance.toggleExpenseActive(sampleExpenses[0]);
    await fixture.whenStable();
    expect(updateRecurringExpense).toHaveBeenCalledWith('exp-1', {
      descricao: 'Internet Fibra',
      valor: 150,
      diaVencimento: 20,
      ativo: false,
    });

    fixture.componentInstance.requestDeleteExpense(sampleExpenses[0]);
    fixture.detectChanges();
    expect(fixture.componentInstance.expenseDeleteOpen()).toBe(true);
    fixture.componentInstance.deleteExpense();
    await fixture.whenStable();
    expect(deleteRecurringExpense).toHaveBeenCalledWith('exp-1');
    expect(fixture.componentInstance.expenseDeleteOpen()).toBe(false);
  });

  it('exibe estado de erro quando a carga falha e permite tentar novamente', async () => {
    const listIncomes = vi
      .fn()
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })))
      .mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [PlanningComponent],
      providers: [
        provideRouter([]),
        {
          provide: PlanningService,
          useValue: {
            listIncomes,
            listRecurringExpenses: () => of([]),
          },
        },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(PlanningComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;

    expect(page.textContent).toContain('Não foi possível carregar o planejamento');
    page.querySelector<HTMLButtonElement>('button.text-action')!.click();
    fixture.detectChanges();
    expect(listIncomes).toHaveBeenCalledTimes(2);
    expect(page.textContent).toContain('Nenhuma fonte de renda prevista');
  });

  it('permite alternar status e excluir fonte de renda com confirmação', async () => {
    const deleteResponse = new Subject<void>();
    const updateIncome = vi.fn((id, req) => of({ id, ...req }));
    const deleteIncome = vi.fn(() => deleteResponse);

    await TestBed.configureTestingModule({
      imports: [PlanningComponent],
      providers: [
        provideRouter([]),
        {
          provide: PlanningService,
          useValue: {
            listIncomes: () => of(sampleIncomes),
            listRecurringExpenses: () => of([]),
            updateIncome,
            deleteIncome,
          },
        },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(PlanningComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    stubDialogs(page);

    fixture.componentInstance.toggleIncomeActive(sampleIncomes[0]);
    await fixture.whenStable();
    expect(updateIncome).toHaveBeenCalledWith('inc-1', {
      fonte: 'Salário CLT',
      valorBase: 5000,
      ativo: false,
    });

    fixture.componentInstance.requestDeleteIncome(sampleIncomes[0]);
    fixture.detectChanges();
    expect(fixture.componentInstance.incomeDeleteOpen()).toBe(true);
    fixture.componentInstance.deleteIncome();
    expect(deleteIncome).toHaveBeenCalledTimes(1);
    deleteResponse.next();
    deleteResponse.complete();
    await fixture.whenStable();
    expect(fixture.componentInstance.incomeDeleteOpen()).toBe(false);
  });
});
