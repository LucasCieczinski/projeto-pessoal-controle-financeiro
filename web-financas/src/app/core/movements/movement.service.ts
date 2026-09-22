import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { CreateMovementRequest, Movement, MovementSummary } from './movement.models';

@Injectable({ providedIn: 'root' })
export class MovementService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly apiUrl = environment.apiUrl;

  forMonth(month: string): Observable<Movement[]> {
    return this.http.get<Movement[]>(`${this.apiUrl}/movimentacoes`, {
      params: new HttpParams().set('mes', month),
    });
  }

  create(request: CreateMovementRequest): Observable<Movement> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.post<Movement>(`${this.apiUrl}/movimentacoes`, request)),
    );
  }

  update(id: string, request: CreateMovementRequest): Observable<Movement> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.put<Movement>(`${this.apiUrl}/movimentacoes/${id}`, request)),
    );
  }

  delete(id: string): Observable<void> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.delete<void>(`${this.apiUrl}/movimentacoes/${id}`)),
    );
  }

  summary(month: string): Observable<MovementSummary> {
    return this.http.get<MovementSummary>(`${this.apiUrl}/movimentacoes/resumo`, {
      params: new HttpParams().set('mes', month),
    });
  }
}
