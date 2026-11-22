import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { Income, IncomeRequest, RecurringExpense, RecurringExpenseRequest } from './planning.models';

@Injectable({ providedIn: 'root' })
export class PlanningService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly apiUrl = environment.apiUrl;

  listIncomes(): Observable<Income[]> {
    return this.http.get<Income[]>(`${this.apiUrl}/rendas`);
  }

  createIncome(request: IncomeRequest): Observable<Income> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.post<Income>(`${this.apiUrl}/rendas`, request)),
    );
  }

  updateIncome(id: string, request: IncomeRequest): Observable<Income> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.put<Income>(`${this.apiUrl}/rendas/${id}`, request)),
    );
  }

  deleteIncome(id: string): Observable<void> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.delete<void>(`${this.apiUrl}/rendas/${id}`)),
    );
  }

  listRecurringExpenses(): Observable<RecurringExpense[]> {
    return this.http.get<RecurringExpense[]>(`${this.apiUrl}/gastos-recorrentes`);
  }

  createRecurringExpense(request: RecurringExpenseRequest): Observable<RecurringExpense> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.post<RecurringExpense>(`${this.apiUrl}/gastos-recorrentes`, request)),
    );
  }

  updateRecurringExpense(id: string, request: RecurringExpenseRequest): Observable<RecurringExpense> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.put<RecurringExpense>(`${this.apiUrl}/gastos-recorrentes/${id}`, request)),
    );
  }

  deleteRecurringExpense(id: string): Observable<void> {
    return this.authService.ensureCsrf().pipe(
      switchMap(() => this.http.delete<void>(`${this.apiUrl}/gastos-recorrentes/${id}`)),
    );
  }
}
