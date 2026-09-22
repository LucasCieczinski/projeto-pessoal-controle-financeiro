import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { MovementService } from './movement.service';

describe('MovementService', () => {
  it('lista apenas o mês solicitado e prepara CSRF antes de criar', () => {
    const ensureCsrf = vi.fn(() => of(void 0));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { ensureCsrf } },
      ],
    });
    const service = TestBed.inject(MovementService);
    const http = TestBed.inject(HttpTestingController);

    service.forMonth('2026-09').subscribe();
    const listRequest = http.expectOne((request) => request.url === `${environment.apiUrl}/movimentacoes`);
    expect(listRequest.request.params.get('mes')).toBe('2026-09');
    listRequest.flush([]);

    const movement = {
      descricao: 'Mercado', valor: 42.5, tipo: 'SAIDA' as const,
      categoria: 'Alimentação', dataMovimentacao: '2026-09-10',
    };
    service.create(movement).subscribe();
    expect(ensureCsrf).toHaveBeenCalledOnce();
    const createRequest = http.expectOne(`${environment.apiUrl}/movimentacoes`);
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(movement);
    createRequest.flush({ id: 'mov-1', ...movement, dataCadastro: '2026-09-10T12:00:00' });

    service.update('mov-1', movement).subscribe();
    const updateRequest = http.expectOne(`${environment.apiUrl}/movimentacoes/mov-1`);
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual(movement);
    updateRequest.flush({ id: 'mov-1', ...movement });

    service.delete('mov-1').subscribe();
    const deleteRequest = http.expectOne(`${environment.apiUrl}/movimentacoes/mov-1`);
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(null);
    expect(ensureCsrf).toHaveBeenCalledTimes(3);

    service.summary('2025-01').subscribe();
    const summaryRequest = http.expectOne(`${environment.apiUrl}/movimentacoes/resumo?mes=2025-01`);
    expect(summaryRequest.request.method).toBe('GET');
    summaryRequest.flush({});
    http.verify();
  });
});
