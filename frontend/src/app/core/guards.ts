import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Rol } from './models';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.logueado() ? true : inject(Router).createUrlTree(['/login']);
};

/** Deja pasar solo a los roles indicados; al resto lo manda al swipe. */
export function rolGuard(...roles: Rol[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    if (!auth.logueado()) {
      return inject(Router).createUrlTree(['/login']);
    }
    return auth.tieneRol(...roles) ? true : inject(Router).createUrlTree(['/swipe']);
  };
}
