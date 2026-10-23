import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Agrega "Authorization: Basic ..." a las llamadas a /api y desloguea ante un 401. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const credenciales = auth.credenciales();
  const pedido =
    credenciales && req.url.startsWith('/api')
      ? req.clone({ setHeaders: { Authorization: `Basic ${credenciales}` } })
      : req;

  return next(pedido).pipe(
    catchError((e: HttpErrorResponse) => {
      if (e.status === 401 && auth.logueado()) {
        auth.logout();
      }
      return throwError(() => e);
    }),
  );
};
