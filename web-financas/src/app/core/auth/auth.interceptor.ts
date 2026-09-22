import { DOCUMENT } from '@angular/common';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

const readCsrfCookie = (cookieHeader: string): string | undefined => {
  const entry = cookieHeader.split(';').map((part) => part.trim()).find((part) => part.startsWith('XSRF-TOKEN='));
  if (!entry) {
    return undefined;
  }
  try {
    return decodeURIComponent(entry.substring('XSRF-TOKEN='.length));
  } catch {
    return undefined;
  }
};

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const document = inject(DOCUMENT);
  const authService = inject(AuthService);
  const apiBaseUrl = environment.apiUrl.replace(/\/+$/, '');
  const isApiRequest = request.url.startsWith(`${apiBaseUrl}/`);
  if (!isApiRequest) {
    return next(request);
  }

  const withCookies = () => {
    const isUnsafe = !['GET', 'HEAD', 'OPTIONS'].includes(request.method);
    const csrfToken = isUnsafe ? readCsrfCookie(document.cookie) : undefined;
    return request.clone({
      withCredentials: true,
      setHeaders: csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {},
    });
  };
  const canRefresh = ![
    `${apiBaseUrl}/auth/login`,
    `${apiBaseUrl}/auth/refresh`,
    `${apiBaseUrl}/auth/logout`,
    `${apiBaseUrl}/auth/csrf`,
  ].includes(request.url) && !(request.method === 'POST' && request.url === `${apiBaseUrl}/users`);

  return next(withCookies()).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || !canRefresh) {
        return throwError(() => error);
      }
      return authService.refreshSession().pipe(
        catchError(() => throwError(() => error)),
        switchMap(() => next(withCookies())),
      );
    }),
  );
};
