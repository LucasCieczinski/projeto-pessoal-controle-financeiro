import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('explica os campos obrigatórios sem chamar a API', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.componentInstance.submit();
    fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('#login-email-error')?.textContent).toContain('Informe seu e-mail.');
    expect(page.querySelector('#login-password-error')?.textContent).toContain('Informe sua senha.');
    expect(page.querySelector('#login-email')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('permite mostrar e ocultar a senha', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    const password = page.querySelector<HTMLInputElement>('#login-password')!;
    const reveal = page.querySelector<HTMLButtonElement>('.reveal')!;
    expect(password.type).toBe('password');

    reveal.click();
    fixture.detectChanges();
    expect(password.type).toBe('text');
    expect(reveal.getAttribute('aria-pressed')).toBe('true');
  });
});
