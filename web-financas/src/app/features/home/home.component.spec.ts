import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { MovementSummary } from '../../core/movements/movement.models';
import { MovementService } from '../../core/movements/movement.service';
import { Income, RecurringExpense } from '../../core/planning/planning.models';
import { PlanningService } from '../../core/planning/planning.service';
import { HomeComponent } from './home.component';

const empty: MovementSummary = {
  mes: '2025-01', entradas: 0, saidas: 0, resultado: 0, saldoAnterior: 0,
  saldoAcumulado: 0, quantidade: 0, totalRegistros: 0, ultimasMovimentacoes: [],
};

async function setup(
  summary: () => ReturnType<MovementService['summary']>,
  incomes: Income[] = [],
  recurringExpenses: RecurringExpense[] = [],
) {
  await TestBed.configureTestingModule({
    imports: [HomeComponent],
    providers: [
      provideRouter([]),
      { provide: MovementService, useValue: { summary } },
      {
        provide: PlanningService,
        useValue: {
          listIncomes: () => of(incomes),
          listRecurringExpenses: () => of(recurringExpenses),
        },
      },
      { provide: AuthService, useValue: {
        currentUser: () => of({ id: 'interno', nome: 'Ana Silva', email: 'ana@email.com' }),
        logout: () => of(void 0),
      } },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(HomeComponent);
  fixture.detectChanges();
  return fixture;
}

describe('HomeComponent', () => {
  it('oferece o primeiro registro sem inventar valores para a conta vazia', async () => {
    const fixture = await setup(() => of(empty));
    const page = fixture.nativeElement as HTMLElement;
    expect(page.textContent).toContain('Olá, Ana.');
    expect(page.textContent).toContain('O primeiro registro muda a perspectiva');
    expect(page.textContent).not.toContain('R$');
    expect(page.textContent).not.toContain('interno');
    expect(page.querySelector('.primary-action')?.getAttribute('href')).toContain('novo=1');
  });

  it('distingue saldo acumulado, resultado do mês e registros recentes reais', async () => {
    const fixture = await setup(() => of({
      ...empty, entradas: 100, saidas: 20, resultado: 80, saldoAnterior: 200,
      saldoAcumulado: 280, quantidade: 2, totalRegistros: 3,
      ultimasMovimentacoes: [
        { id: '1', descricao: 'Mercado', valor: 20, tipo: 'SAIDA', categoria: 'Alimentação', dataMovimentacao: '2025-01-10', dataCadastro: '' },
        { id: '2', descricao: 'Freelance', valor: 100, tipo: 'ENTRADA', categoria: 'Trabalho', dataMovimentacao: '2025-01-09', dataCadastro: '' },
      ],
    }));
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('.balance-value')?.textContent).toContain('280,00');
    expect(page.querySelector('.period-result dd')?.textContent).toContain('80,00');
    expect(page.querySelectorAll('.recent-list li')).toHaveLength(2);
    expect(page.textContent).toContain('Não é um saldo bancário sincronizado');
    expect(page.textContent).toContain('Mercado');
  });

  it('mantém o saldo anterior em meses sem movimentações', async () => {
    const fixture = await setup(() => of({ ...empty, totalRegistros: 1, saldoAnterior: -20, saldoAcumulado: -20 }));
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('.balance-value')?.classList.contains('negative')).toBe(true);
    expect(page.textContent).toContain('O saldo anterior permanece');
    expect(page.querySelector('.empty-state')).toBeNull();
  });

  it('ignora respostas antigas, mostra erro sem zeros e permite tentar novamente', async () => {
    const first = new Subject<MovementSummary>();
    const second = new Subject<MovementSummary>();
    const summary = vi.fn().mockReturnValueOnce(first).mockReturnValueOnce(second).mockReturnValue(of(empty));
    const fixture = await setup(summary);
    expect(fixture.nativeElement.textContent).toContain('Organizando seu resumo');
    fixture.componentInstance.changeMonth(-1);
    second.error(new HttpErrorResponse({ status: 0 }));
    first.next({ ...empty, saldoAcumulado: 999, totalRegistros: 1 });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Seu resumo não carregou');
    expect(fixture.nativeElement.textContent).not.toContain('999');
    expect(fixture.nativeElement.textContent).not.toContain('R$');
    (fixture.nativeElement.querySelector('button.text-action') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('O primeiro registro');
    expect(summary).toHaveBeenCalledTimes(3);
  });

  it('exibe indicador de Renda Prevista vs. Gastos Fixos com link para /planejamento', async () => {
    const fixture = await setup(
      () => of({ ...empty, totalRegistros: 1, entradas: 4000, saidas: 1500, resultado: 2500, saldoAcumulado: 2500 }),
      [
        { id: 'i1', fonte: 'Salário', valorBase: 4000, ativo: true },
        { id: 'i2', fonte: 'Bônus inativo', valorBase: 1000, ativo: false },
      ],
      [
        { id: 'e1', descricao: 'Aluguel', valor: 1200, diaVencimento: 10, ativo: true },
        { id: 'e2', descricao: 'Academia inativa', valor: 150, diaVencimento: 15, ativo: false },
      ],
    );
    const page = fixture.nativeElement as HTMLElement;
    const planningCard = page.querySelector('.planning-overview-card')!;
    expect(planningCard).toBeTruthy();
    expect(planningCard.textContent).toContain('Planejamento fixo mensal');
    expect(planningCard.textContent).toContain('4.000,00');
    expect(planningCard.textContent).toContain('1.200,00');
    expect(planningCard.textContent).toContain('2.800,00');
    expect(planningCard.textContent).toContain('30% da renda comprometida');
    expect(planningCard.querySelector('a')?.getAttribute('href')).toBe('/planejamento');
  });
});
