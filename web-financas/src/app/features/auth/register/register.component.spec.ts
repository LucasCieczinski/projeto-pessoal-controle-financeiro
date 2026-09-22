import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('mostra mensagens junto aos campos vazios', () => {
    const fixture = TestBed.createComponent(RegisterComponent);
    fixture.componentInstance.submit();
    fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('#register-name-error')?.textContent).toContain('pelo menos 2 caracteres');
    expect(page.querySelector('#register-email-error')?.textContent).toContain('Informe seu e-mail.');
    expect(page.querySelector('#register-password-error')?.textContent).toContain('Crie uma senha.');
    expect(page.querySelector('#register-confirmation-error')?.textContent).toContain('Confirme sua senha.');
  });

  it('rejeita nome em branco e senhas diferentes', () => {
    const fixture = TestBed.createComponent(RegisterComponent);
    const form = fixture.componentInstance.form;
    form.setValue({
      nome: '   ',
      email: 'pessoa@email.com',
      senha: 'senha-segura',
      confirmacaoSenha: 'outra-senha',
    });

    expect(form.invalid).toBe(true);
    expect(form.controls.nome.hasError('shortName')).toBe(true);
    expect(form.hasError('passwordsDoNotMatch')).toBe(true);
  });

  it('aceita dados válidos sem exigir concordância com termos inexistentes', () => {
    const fixture = TestBed.createComponent(RegisterComponent);
    const form = fixture.componentInstance.form;
    form.setValue({
      nome: 'Ana Silva',
      email: 'ana@email.com',
      senha: 'senha-segura',
      confirmacaoSenha: 'senha-segura',
    });

    expect(form.valid).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Termos de Uso');
  });
});
