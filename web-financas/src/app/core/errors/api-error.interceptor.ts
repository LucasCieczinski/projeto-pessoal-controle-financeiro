import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ErrorNotificationService, ToastTone } from './error-notification.service';

/**
 * Endpoints que já exibem feedback inline nos formulários (credenciais
 * inválidas, validação de cadastro, etc). Exibi-los também como toast
 * duplicaria a mensagem na tela.
 */
const HANDLED_BY_COMPONENTS = new Set([
  '/auth/login',
  '/auth/refresh',
  '/auth/logout',
  '/auth/csrf',
]);

interface Feedback {
  message: string;
  tone: ToastTone;
}

/** Traduz os status tratados globalmente (401, 403, 5xx) para texto educado. */
const feedbackFor = (error: HttpErrorResponse): Feedback | null => {
  switch (error.status) {
    case 401:
      return { message: 'Sua sessão expirou. Entre novamente para continuar.', tone: 'warning' };
    case 403:
      return { message: 'Você não tem permissão para realizar esta ação.', tone: 'warning' };
    default:
      // Erros 5xx nunca são esperados pela tela: avisam que o servidor falhou.
      // 400/404/0 e demais status já têm tratamento inline específico.
      return error.status >= 500
        ? { message: 'Não conseguimos processar sua solicitação agora. Tente novamente em instantes.', tone: 'error' }
        : null;
  }
};

/**
 * Interceptor central de erros de API (Onda 1, item "Tratamento Global de Erros").
 *
 * Exibe um toast educado para 401 (sessão expirada), 403 (sem permissão) e
 * 5xx (falha do servidor), sempre após o `authInterceptor` tentar renovar a
 * sessão — por isso ele deve ser registrado ANTES dele em `app.config.ts`
 * (o interceptor mais externo só vê o erro final, depois do refresh).
 *
 * O erro NÃO é engolido: ele segue propagado para os componentes.
 */
export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const notifications = inject(ErrorNotificationService);
  const apiBaseUrl = environment.apiUrl.replace(/\/+$/, '');
  const isApiRequest = request.url.startsWith(`${apiBaseUrl}/`);
  const handledByComponents = [...HANDLED_BY_COMPONENTS].some((path) => request.url === `${apiBaseUrl}${path}`)
    || (request.method === 'POST' && request.url === `${apiBaseUrl}/users`);

  if (!isApiRequest || handledByComponents) {
    return next(request);
  }

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      const feedback = feedbackFor(error);
      if (feedback) {
        notifications.show(feedback.message, feedback.tone);
      }
      return throwError(() => error);
    }),
  );
};
