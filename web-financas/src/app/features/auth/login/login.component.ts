import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';
import { AuthShellComponent } from '../../../shared/auth-shell/auth-shell.component';

@Component({
  selector: 'app-login',
  imports: [RouterLink, ReactiveFormsModule, AuthShellComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly form = this.formBuilder.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
    lembrar: false,
  });

  showPassword = false;
  isSubmitting = false;
  errorMessage = '';
  readonly successMessage =
    this.route.snapshot.queryParamMap.get('cadastro') === 'sucesso'
      ? 'Conta criada com sucesso. Entre para continuar.'
      : '';

  submit(): void {
    if (this.isSubmitting) {
      return;
    }

    this.errorMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { email, senha, lembrar } = this.form.getRawValue();
    this.isSubmitting = true;
    this.authService
      .login({ email: email.trim(), senha }, lembrar)
      .pipe(finalize(() => (this.isSubmitting = false)))
      .subscribe({
        next: () => void this.router.navigate(['/inicio']),
        error: (error: HttpErrorResponse) => {
          if (error.status === 0) {
            this.errorMessage = 'Não conseguimos conectar à API. Confira se ela está em execução.';
            return;
          }
          const apiError = error.error as ApiError | undefined;
          this.errorMessage = apiError?.mensagem ?? 'Não foi possível entrar. Tente novamente.';
        },
      });
  }
}
