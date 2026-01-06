import { inject } from '@angular/core';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth';

export const AuthGuard = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const router = inject(Router);
  const authService = inject(AuthService);
  const token = authService.getToken();
  const user = authService.getUserFromStorage();
  const status = localStorage.getItem('userStatus');

  if (token && user) {
    if (status === 'PENDING') {
       if (state.url.includes('/pending-approval')) {
         return true;
       }
       router.navigate(['/pending-approval']);
       return false;
    }
    if (status === 'ACTIVE' && state.url.includes('/pending-approval')) {
       router.navigate(['/consumer/dashboard']);
       return false;
    }
    return true;
  }
  router.navigate(['/login']);
  return false;
};