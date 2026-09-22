import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controller.verify();
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
  });

  it('envia cookies apenas à API, sem expor JWT em cabeçalhos', () => {
    http.post(`${environment.apiUrl}/auth/login`, {}).subscribe();
    http.get(`${environment.apiUrl}/users/me`).subscribe();
    http.get('https://example.org/outro-servico').subscribe();

    const login = controller.expectOne(`${environment.apiUrl}/auth/login`);
    const profile = controller.expectOne(`${environment.apiUrl}/users/me`);
    const external = controller.expectOne('https://example.org/outro-servico');
    expect(login.request.withCredentials).toBe(true);
    expect(profile.request.withCredentials).toBe(true);
    expect(profile.request.headers.has('Authorization')).toBe(false);
    expect(external.request.withCredentials).toBe(false);
    login.flush(null);
    profile.flush({});
    external.flush({});
  });

  it('envia token CSRF apenas nas operações da API que alteram dados', () => {
    document.cookie = 'XSRF-TOKEN=token-csrf; Path=/';
    http.post(`${environment.apiUrl}/auth/logout`, null).subscribe();

    const logout = controller.expectOne(`${environment.apiUrl}/auth/logout`);
    expect(logout.request.headers.get('X-XSRF-TOKEN')).toBe('token-csrf');
    logout.flush(null);
  });

  it('renova o cookie e repete uma requisição protegida após 401', () => {
    document.cookie = 'XSRF-TOKEN=token-csrf; Path=/';
    let received = false;
    http.get(`${environment.apiUrl}/users/me`).subscribe(() => (received = true));

    controller.expectOne(`${environment.apiUrl}/users/me`).flush({}, { status: 401, statusText: 'Unauthorized' });
    controller.expectOne(`${environment.apiUrl}/auth/csrf`).flush(null);
    const refresh = controller.expectOne(`${environment.apiUrl}/auth/refresh`);
    expect(refresh.request.withCredentials).toBe(true);
    expect(refresh.request.headers.get('X-XSRF-TOKEN')).toBe('token-csrf');
    refresh.flush(null);
    controller.expectOne(`${environment.apiUrl}/users/me`).flush({ nome: 'Ana' });
    expect(received).toBe(true);
  });

  it('compartilha uma única renovação entre requisições simultâneas', () => {
    http.get(`${environment.apiUrl}/users/me`).subscribe();
    http.get(`${environment.apiUrl}/users/me`).subscribe();

    const initial = controller.match(`${environment.apiUrl}/users/me`);
    expect(initial).toHaveLength(2);
    for (const request of initial) {
      request.flush({}, { status: 401, statusText: 'Unauthorized' });
    }
    controller.expectOne(`${environment.apiUrl}/auth/csrf`).flush(null);
    controller.expectOne(`${environment.apiUrl}/auth/refresh`).flush(null);
    const retries = controller.match(`${environment.apiUrl}/users/me`);
    expect(retries).toHaveLength(2);
    for (const request of retries) {
      request.flush({ nome: 'Ana' });
    }
  });
});
