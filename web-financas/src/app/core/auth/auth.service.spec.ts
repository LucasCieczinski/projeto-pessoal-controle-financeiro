import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  it('envia a opção de continuar conectado sem armazenar JWT no navegador', () => {
    localStorage.setItem('financas.jwt', 'token-antigo');
    sessionStorage.setItem('financas.jwt', 'token-antigo');
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(AuthService);
    const requests = TestBed.inject(HttpTestingController);

    service.login({ email: 'ana@teste.com', senha: 'senha123' }, true).subscribe();
    const login = requests.expectOne(`${environment.apiUrl}/auth/login`);
    expect(login.request.body).toEqual({ email: 'ana@teste.com', senha: 'senha123', remember: true });
    login.flush(null);
    expect(localStorage.getItem('financas.jwt')).toBeNull();
    expect(sessionStorage.getItem('financas.jwt')).toBeNull();
    requests.verify();
  });
});
