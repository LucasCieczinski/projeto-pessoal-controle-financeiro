import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./features/landing/landing.component').then((component) => component.LandingComponent),
    title: 'À Tona | Seu dinheiro, com mais clareza',
  },
  { path: 'login', component: LoginComponent, title: 'Entrar | À Tona' },
  { path: 'cadastro', component: RegisterComponent, title: 'Criar conta | À Tona' },
  {
    path: 'inicio',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/home/home.component').then((component) => component.HomeComponent),
    title: 'Resumo | À Tona',
  },
  {
    path: 'movimentacoes',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/movements/movements.component').then((component) => component.MovementsComponent),
    title: 'Movimentações | À Tona',
  },
  { path: '**', redirectTo: '' },
];
