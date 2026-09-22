import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { MovementSummary } from '../../core/movements/movement.models';
import { MovementService } from '../../core/movements/movement.service';
import { PrivateHeaderComponent } from '../../shared/private-header/private-header.component';
import { PeriodNavigationComponent } from '../../shared/period-navigation/period-navigation.component';

@Component({
  selector: 'app-home',
  imports: [RouterLink, PrivateHeaderComponent, PeriodNavigationComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly movements = inject(MovementService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private loadSequence = 0;

  year = new Date().getFullYear();
  monthIndex = new Date().getMonth();
  firstName = '';
  summary?: MovementSummary;
  isLoading = true;
  errorMessage = '';
  sessionError = '';

  ngOnInit(): void {
    const month = this.route.snapshot.queryParamMap.get('mes');
    if (month && /^[1-9][0-9]{3}-(0[1-9]|1[0-2])$/.test(month) && month <= this.monthKey) {
      const [year, number] = month.split('-').map(Number);
      this.year = year;
      this.monthIndex = number - 1;
    }
    this.authService.currentUser().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (user) => {
        this.firstName = user.nome.trim().split(/\s+/)[0];
        this.changeDetector.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 401) void this.router.navigate(['/login']);
      },
    });
    this.loadSummary();
  }

  get monthKey(): string {
    return this.year + '-' + String(this.monthIndex + 1).padStart(2, '0');
  }

  get monthLabel(): string {
    return new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })
      .format(new Date(this.year, this.monthIndex, 1));
  }

  get monthHeading(): string {
    return this.monthLabel.charAt(0).toUpperCase() + this.monthLabel.slice(1);
  }

  get isCurrentMonth(): boolean {
    const today = new Date();
    return this.year === today.getFullYear() && this.monthIndex === today.getMonth();
  }

  get balanceDate(): string {
    const day = this.isCurrentMonth ? new Date().getDate() : new Date(this.year, this.monthIndex + 1, 0).getDate();
    return String(day).padStart(2, '0') + '/' + String(this.monthIndex + 1).padStart(2, '0') + '/' + this.year;
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  }

  formatSignedMoney(value: number): string {
    const sign = value > 0 ? '+ ' : value < 0 ? '− ' : '';
    return sign + this.formatMoney(Math.abs(value));
  }

  formatDate(value: string): string {
    return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' })
      .format(new Date(value + 'T12:00:00'));
  }

  loadSummary(): void {
    const sequence = ++this.loadSequence;
    this.isLoading = true;
    this.errorMessage = '';
    this.movements.summary(this.monthKey).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (summary) => {
        if (sequence !== this.loadSequence) return;
        this.summary = summary;
        this.isLoading = false;
        this.changeDetector.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        if (sequence !== this.loadSequence) return;
        this.summary = undefined;
        this.isLoading = false;
        this.errorMessage = error.status === 0
          ? 'Confira sua conexão e tente novamente. Seus registros continuam guardados.'
          : 'Não conseguimos consultar seus registros agora. Tente novamente em instantes.';
        this.changeDetector.markForCheck();
        if (error.status === 401) void this.router.navigate(['/login']);
      },
    });
  }

  changeMonth(offset: number): void {
    if (offset > 0 && this.isCurrentMonth) return;
    const next = new Date(this.year, this.monthIndex + offset, 1);
    this.year = next.getFullYear();
    this.monthIndex = next.getMonth();
    this.loadSummary();
  }

  goToCurrentMonth(): void {
    const today = new Date();
    this.year = today.getFullYear();
    this.monthIndex = today.getMonth();
    this.loadSummary();
  }

  logout(): void {
    this.authService.logout().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => void this.router.navigate(['/login']),
      error: () => {
        this.sessionError = 'Não conseguimos encerrar sua sessão. Tente novamente.';
        this.changeDetector.markForCheck();
      },
    });
  }
}
