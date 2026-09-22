import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { finalize, Observable, shareReplay, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, RegisterRequest, UserResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private refreshInFlight?: Observable<void>;

  constructor() {
    // Remove JWTs antigos que a versão anterior guardava no navegador.
    try {
      localStorage.removeItem('financas.jwt');
      sessionStorage.removeItem('financas.jwt');
    } catch {
      // Armazenamento pode estar bloqueado; a autenticação atual não depende dele.
    }
  }

  login(credentials: LoginRequest, remember: boolean): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/login`, { ...credentials, remember });
  }

  register(data: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.apiUrl}/users`, data);
  }

  currentUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/users/me`);
  }

  ensureCsrf(): Observable<void> {
    return this.http.get<void>(`${this.apiUrl}/auth/csrf`);
  }

  refreshSession(): Observable<void> {
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.ensureCsrf().pipe(
        switchMap(() => this.http.post<void>(`${this.apiUrl}/auth/refresh`, null)),
        finalize(() => (this.refreshInFlight = undefined)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }
    return this.refreshInFlight;
  }

  logout(): Observable<void> {
    return this.ensureCsrf().pipe(
      switchMap(() => this.http.post<void>(`${this.apiUrl}/auth/logout`, null)),
    );
  }
}
