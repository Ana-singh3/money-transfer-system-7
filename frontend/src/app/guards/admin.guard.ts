import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const AdminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  const currentUser = authService.currentUserValue;
  if (currentUser && currentUser.role === 'ROLE_ADMIN') {
    return true;
  }

  router.navigate(['/auth'], { queryParams: { returnUrl: state.url } });
  return false;
};

