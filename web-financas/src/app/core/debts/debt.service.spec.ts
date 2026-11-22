import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { DebtRequest } from './debt.models';
import { DebtService } from './debt.service';

describe('DebtService', () => {
  it('gerencia dívidas e parcelamentos preparando CSRF antes de todas as mutações', () => {
    const ensureCsrf = vi.fn(() => of(void 0));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { ensureCsrf } },
      ],
    });
    const service = TestBed.inject(DebtService);
    const http = TestBed.inject(HttpTestingController);

    service.listDebts().subscribe();
    const listRequest = http.expectOne(`${environment.apiUrl}/dividas`);
    expect(listRequest.request.method).toBe('GET');
    listRequest.flush([]);
    expect(ensureCsrf).not.toHaveBeenCalled();

    const debtRequest: DebtRequest = {
      credor: 'Banco Itaú',
      valorTotal: 4800,
      totalParcelas: 12,
      parcelasPagas: 3,
      status: 'ABERTA',
    };
    service.createDebt(debtRequest).subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(1);
    const createRequest = http.expectOne(`${environment.apiUrl}/dividas`);
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(debtRequest);
    createRequest.flush({ id: 'debt-1', ...debtRequest });

    service.updateDebt('debt-1', { ...debtRequest, status: 'ATRASADA' }).subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(2);
    const updateRequest = http.expectOne(`${environment.apiUrl}/dividas/debt-1`);
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual({ ...debtRequest, status: 'ATRASADA' });
    updateRequest.flush({ id: 'debt-1', ...debtRequest, status: 'ATRASADA' });

    service.payInstallment('debt-1').subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(3);
    const patchRequest = http.expectOne(`${environment.apiUrl}/dividas/debt-1/pagar-parcela`);
    expect(patchRequest.request.method).toBe('PATCH');
    expect(patchRequest.request.body).toEqual({});
    patchRequest.flush({ id: 'debt-1', ...debtRequest, parcelasPagas: 4, status: 'ABERTA' });

    service.deleteDebt('debt-1').subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(4);
    const deleteRequest = http.expectOne(`${environment.apiUrl}/dividas/debt-1`);
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(null);

    http.verify();
  });
});
