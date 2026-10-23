import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiError } from './models';

@Injectable({ providedIn: 'root' })
export class NotificadorService {
  private readonly snack = inject(MatSnackBar);

  ok(mensaje: string): void {
    this.snack.open(mensaje, 'OK', { duration: 3500, panelClass: 'snack-ok' });
  }

  error(e: unknown): void {
    this.snack.open(mensajeDeError(e), 'Cerrar', { duration: 6000, panelClass: 'snack-error' });
  }
}

export function mensajeDeError(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    const cuerpo = e.error as ApiError | null;
    if (cuerpo?.mensaje) return cuerpo.mensaje;
    if (e.status === 0) return 'No hay conexión con el backend. ¿Está levantado en el puerto 8080?';
  }
  return 'Algo salió mal. Probá de nuevo.';
}
