import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { apiErrorInterceptor } from './api-error.interceptor';
import { ErrorNotificationService } from './error-notification.service';

describe('apiErrorInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let notifications: ErrorNotificationService;

  const visibleMessages = () => notifications.toasts().map((toast) => toast.message);

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([apiErrorInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    notifications = TestBed.inject(ErrorNotificationService);
  });

  afterEach(() => {
    controller.verify();
    vi.useRealTimers();
  });

  function requestAndFail(url: string, status: number, method: 'get' | 'post' = 'get'): void {
    let captured: unknown;
    (method === 'get' ? http.get(url) : http.post(url, null)).subscribe({ error: (error) => (captured = error) });
    controller.expectOne(url).flush({}, { status, statusText: String(status) });
    expect(captured).toBeInstanceOf(HttpErrorResponse);
    expect((captured as HttpErrorResponse).status).toBe(status);
  }

  it('avisa quando a sessão expira (401)', () => {
    requestAndFail(`${environment.apiUrl}/movements?month=2026-10`, 401);
    expect(visibleMessages()).toEqual(['Sua sessão expirou. Entre novamente para continuar.']);
  });

  it('avisa quando a ação não é permitida (403)', () => {
    requestAndFail(`${environment.apiUrl}/movements/1`, 403, 'post');
    expect(visibleMessages()).toEqual(['Você não tem permissão para realizar esta ação.']);
  });

  it('avisa quando o servidor falha (5xx)', () => {
    requestAndFail(`${environment.apiUrl}/movements/summary?month=2026-10`, 500);
    expect(visibleMessages()).toEqual(['Não conseguimos processar sua solicitação agora. Tente novamente em instantes.']);
  });

  it('não avisa erros com tratamento inline nos componentes (400/404)', () => {
    requestAndFail(`${environment.apiUrl}/movements/1`, 404);
    requestAndFail(`${environment.apiUrl}/movements`, 400, 'post');
    expect(visibleMessages()).toEqual([]);
  });

  it('não duplica o feedback de credenciais inválidas no login', () => {
    requestAndFail(`${environment.apiUrl}/auth/login`, 401, 'post');
    expect(visibleMessages()).toEqual([]);
  });

  it('ignora requisições fora da API', () => {
    requestAndFail('https://example.org/pagamento', 500);
    expect(visibleMessages()).toEqual([]);
  });

  it('permite fechar o aviso manualmente', () => {
    requestAndFail(`${environment.apiUrl}/movements`, 500);
    expect(notifications.toasts()).toHaveLength(1);
    notifications.dismiss(notifications.toasts()[0].id);
    expect(notifications.toasts()).toHaveLength(0);
  });

  it('descarta automaticamente avisos antigos', () => {
    requestAndFail(`${environment.apiUrl}/movements`, 500);
    expect(notifications.toasts()).toHaveLength(1);
    vi.advanceTimersByTime(6000);
    expect(notifications.toasts()).toHaveLength(0);
  });
});
