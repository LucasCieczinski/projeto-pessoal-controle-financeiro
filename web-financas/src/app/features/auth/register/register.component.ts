import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';
import { AuthShellComponent } from '../../../shared/auth-shell/auth-shell.component';

const passwordsMatch: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('senha')?.value;
  const confirmation = control.get('confirmacaoSenha')?.value;
  return password === confirmation ? null : { passwordsDoNotMatch: true };
};

const meaningfulName: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const name = typeof control.value === 'string' ? control.value.trim() : '';
  return name.length >= 2 ? null : { shortName: true };
};

@Component({
  selector: 'app-register',
  imports: [RouterLink, ReactiveFormsModule, AuthShellComponent],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = this.formBuilder.group(
    {
      nome: ['', [Validators.required, meaningfulName, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      senha: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirmacaoSenha: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  isSubmitting = false;
  errorMessage = '';
  showPassword = false;
  showConfirmation = false;

  submit(): void {
    if (this.isSubmitting) {
      return;
    }

    this.errorMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { nome, email, senha } = this.form.getRawValue();
    this.isSubmitting = true;
    this.authService
      .register({ nome: nome.trim(), email: email.trim(), senha })
      .pipe(finalize(() => (this.isSubmitting = false)))
      .subscribe({
        next: () => void this.router.navigate(['/login'], { queryParams: { cadastro: 'sucesso' } }),
        error: (error: HttpErrorResponse) => {
          if (error.status === 0) {
            this.errorMessage = 'Não conseguimos conectar à API. Confira se ela está em execução.';
            return;
          }
          const apiError = error.error as ApiError | undefined;
          const firstFieldError = apiError?.campos ? Object.values(apiError.campos)[0] : undefined;
          this.errorMessage =
            firstFieldError ?? apiError?.mensagem ?? 'Não foi possível criar a conta.';
        },
      });
  }
}
