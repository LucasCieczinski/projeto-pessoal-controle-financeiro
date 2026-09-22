import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.currentUser().pipe(
    map(() => true),
    catchError((error) => of(error.status === 0 ? true : router.createUrlTree(['/login']))),
  );
};
