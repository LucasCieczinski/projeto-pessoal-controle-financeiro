import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { PlanningService } from './planning.service';

describe('PlanningService', () => {
  it('gerencia fontes de renda e prepara CSRF antes de mutações', () => {
    const ensureCsrf = vi.fn(() => of(void 0));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { ensureCsrf } },
      ],
    });
    const service = TestBed.inject(PlanningService);
    const http = TestBed.inject(HttpTestingController);

    service.listIncomes().subscribe();
    const listRequest = http.expectOne(`${environment.apiUrl}/rendas`);
    expect(listRequest.request.method).toBe('GET');
    listRequest.flush([]);
    expect(ensureCsrf).not.toHaveBeenCalled();

    const incomeRequest = { fonte: 'Salário CLT', valorBase: 5500, ativo: true };
    service.createIncome(incomeRequest).subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(1);
    const createRequest = http.expectOne(`${environment.apiUrl}/rendas`);
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(incomeRequest);
    createRequest.flush({ id: 'inc-1', ...incomeRequest });

    service.updateIncome('inc-1', { ...incomeRequest, ativo: false }).subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(2);
    const updateRequest = http.expectOne(`${environment.apiUrl}/rendas/inc-1`);
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual({ ...incomeRequest, ativo: false });
    updateRequest.flush({ id: 'inc-1', ...incomeRequest, ativo: false });

    service.deleteIncome('inc-1').subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(3);
    const deleteRequest = http.expectOne(`${environment.apiUrl}/rendas/inc-1`);
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(null);

    http.verify();
  });

  it('gerencia gastos recorrentes e prepara CSRF antes de mutações', () => {
    const ensureCsrf = vi.fn(() => of(void 0));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { ensureCsrf } },
      ],
    });
    const service = TestBed.inject(PlanningService);
    const http = TestBed.inject(HttpTestingController);

    service.listRecurringExpenses().subscribe();
    const listRequest = http.expectOne(`${environment.apiUrl}/gastos-recorrentes`);
    expect(listRequest.request.method).toBe('GET');
    listRequest.flush([]);
    expect(ensureCsrf).not.toHaveBeenCalled();

    const expenseRequest = { descricao: 'Aluguel', valor: 1800, diaVencimento: 10, ativo: true };
    service.createRecurringExpense(expenseRequest).subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(1);
    const createRequest = http.expectOne(`${environment.apiUrl}/gastos-recorrentes`);
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(expenseRequest);
    createRequest.flush({ id: 'exp-1', ...expenseRequest });

    service.updateRecurringExpense('exp-1', { ...expenseRequest, valor: 1900 }).subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(2);
    const updateRequest = http.expectOne(`${environment.apiUrl}/gastos-recorrentes/exp-1`);
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual({ ...expenseRequest, valor: 1900 });
    updateRequest.flush({ id: 'exp-1', ...expenseRequest, valor: 1900 });

    service.deleteRecurringExpense('exp-1').subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(3);
    const deleteRequest = http.expectOne(`${environment.apiUrl}/gastos-recorrentes/exp-1`);
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(null);

    http.verify();
  });
});
