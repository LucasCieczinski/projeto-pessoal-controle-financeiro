import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { CreditCardRequest } from './credit-card.models';
import { CreditCardService } from './credit-card.service';

describe('CreditCardService', () => {
  it('gerencia cartões de crédito preparando CSRF antes de todas as mutações', () => {
    const ensureCsrf = vi.fn(() => of(void 0));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { ensureCsrf } },
      ],
    });
    const service = TestBed.inject(CreditCardService);
    const http = TestBed.inject(HttpTestingController);

    service.listCreditCards().subscribe();
    const listRequest = http.expectOne(`${environment.apiUrl}/cartoes`);
    expect(listRequest.request.method).toBe('GET');
    listRequest.flush([]);
    expect(ensureCsrf).not.toHaveBeenCalled();

    const cardRequest: CreditCardRequest = {
      nomeCartao: 'Cartão Nubank',
      limite: 5000,
      diaFechamento: 8,
      diaVencimento: 15,
    };
    service.createCreditCard(cardRequest).subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(1);
    const createRequest = http.expectOne(`${environment.apiUrl}/cartoes`);
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(cardRequest);
    createRequest.flush({ id: 'card-1', ...cardRequest });

    service.updateCreditCard('card-1', { ...cardRequest, limite: 0 }).subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(2);
    const updateRequest = http.expectOne(`${environment.apiUrl}/cartoes/card-1`);
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual({ ...cardRequest, limite: 0 });
    updateRequest.flush({ id: 'card-1', ...cardRequest, limite: 0 });

    service.deleteCreditCard('card-1').subscribe();
    expect(ensureCsrf).toHaveBeenCalledTimes(3);
    const deleteRequest = http.expectOne(`${environment.apiUrl}/cartoes/card-1`);
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(null);

    http.verify();
  });

  it('não dispara mutação HTTP se ensureCsrf falhar', () => {
    const ensureCsrf = vi.fn(() => throwError(() => new Error('csrf error')));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { ensureCsrf } },
      ],
    });
    const service = TestBed.inject(CreditCardService);
    const http = TestBed.inject(HttpTestingController);

    let failed = false;
    service
      .createCreditCard({
        nomeCartao: 'Cartão Inter',
        limite: 2500,
        diaFechamento: 5,
        diaVencimento: 12,
      })
      .subscribe({ error: () => (failed = true) });

    expect(ensureCsrf).toHaveBeenCalledTimes(1);
    expect(failed).toBe(true);
    http.expectNone(`${environment.apiUrl}/cartoes`);
    http.verify();
  });
});
