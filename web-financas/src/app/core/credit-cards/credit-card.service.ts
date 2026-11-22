import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { CreditCard, CreditCardRequest } from './credit-card.models';

@Injectable({ providedIn: 'root' })
export class CreditCardService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly apiUrl = environment.apiUrl;

  listCreditCards(): Observable<CreditCard[]> {
    return this.http.get<CreditCard[]>(`${this.apiUrl}/cartoes`);
  }

  createCreditCard(request: CreditCardRequest): Observable<CreditCard> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.post<CreditCard>(`${this.apiUrl}/cartoes`, request)),
    );
  }

  updateCreditCard(id: string, request: CreditCardRequest): Observable<CreditCard> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.put<CreditCard>(`${this.apiUrl}/cartoes/${id}`, request)),
    );
  }

  deleteCreditCard(id: string): Observable<void> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.delete<void>(`${this.apiUrl}/cartoes/${id}`)),
    );
  }
}
