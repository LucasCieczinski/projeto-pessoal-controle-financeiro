import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { apiErrorInterceptor } from './core/errors/api-error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // Ordem importa: o `apiErrorInterceptor` é o mais externo e só vê o erro
    // final — depois de o `authInterceptor` tentar renovar a sessão (401).
    provideHttpClient(withInterceptors([apiErrorInterceptor, authInterceptor])),
  ],
};
