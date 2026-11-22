import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { Debt, DebtRequest } from './debt.models';

@Injectable({ providedIn: 'root' })
export class DebtService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly apiUrl = environment.apiUrl;

  listDebts(): Observable<Debt[]> {
    return this.http.get<Debt[]>(`${this.apiUrl}/dividas`);
  }

  createDebt(request: DebtRequest): Observable<Debt> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.post<Debt>(`${this.apiUrl}/dividas`, request)),
    );
  }

  updateDebt(id: string, request: DebtRequest): Observable<Debt> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.put<Debt>(`${this.apiUrl}/dividas/${id}`, request)),
    );
  }

  payInstallment(id: string): Observable<Debt> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.patch<Debt>(`${this.apiUrl}/dividas/${id}/pagar-parcela`, {})),
    );
  }

  deleteDebt(id: string): Observable<void> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.delete<void>(`${this.apiUrl}/dividas/${id}`)),
    );
  }
}
