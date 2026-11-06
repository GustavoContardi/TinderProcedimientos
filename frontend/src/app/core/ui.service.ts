import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, TemplateRef, inject, signal } from '@angular/core';
import { NavigationStart, Router } from '@angular/router';
import { filter } from 'rxjs';
import { ApiService } from './api.service';
import { ApiError, Ciclo } from './models';

export type TipoDocumento = 'libreta' | 'necro';

/**
 * Estado de la "carcasa" de la app: hoja inferior, documento a pantalla completa,
 * overlay de match, toast y ciclo actual. Todo se dibuja dentro del marco del teléfono (AppComponent).
 */
@Injectable({ providedIn: 'root' })
export class UiService {
  private readonly api = inject(ApiService);

  readonly toastTxt = signal('');
  readonly toastOn = signal(false);
  readonly toastError = signal(false);
  private toastTimer?: ReturnType<typeof setTimeout>;

  /** Contenido de la hoja inferior: un ng-template declarado en la página que la abre. */
  readonly hoja = signal<TemplateRef<unknown> | null>(null);
  readonly hojaAbierta = signal(false);

  readonly doc = signal<{ tipo: TipoDocumento; id: number } | null>(null);
  readonly docAbierto = signal(false);

  readonly matchNombre = signal('');
  readonly matchOn = signal(false);
  private matchTimer?: ReturnType<typeof setTimeout>;

  /** undefined = cargando; null = no hay ciclo abierto. */
  readonly ciclo = signal<Ciclo | null | undefined>(undefined);

  /** Se incrementa cada vez que algo cambia datos compartidos (firmar acta, casar, etc.). */
  readonly cambios = signal(0);

  constructor() {
    // Si se navega con una hoja abierta, su template pertenece a la página que se destruye.
    inject(Router)
      .events.pipe(filter((e) => e instanceof NavigationStart))
      .subscribe(() => {
        this.hojaAbierta.set(false);
        this.hoja.set(null);
        this.docAbierto.set(false);
      });
  }

  ok(texto: string): void {
    this.mostrarToast(texto, false);
  }

  error(e: unknown): void {
    this.mostrarToast(mensajeDeError(e), true);
  }

  private mostrarToast(texto: string, error: boolean): void {
    clearTimeout(this.toastTimer);
    this.toastTxt.set(texto);
    this.toastError.set(error);
    this.toastOn.set(true);
    this.toastTimer = setTimeout(() => this.toastOn.set(false), error ? 4500 : 2600);
  }

  abrirHoja(t: TemplateRef<unknown>): void {
    this.hoja.set(t);
    this.hojaAbierta.set(true);
  }

  cerrarHoja(): void {
    this.hojaAbierta.set(false);
  }

  abrirDoc(tipo: TipoDocumento, id: number): void {
    this.hojaAbierta.set(false);
    this.doc.set({ tipo, id });
    this.docAbierto.set(true);
  }

  cerrarDoc(): void {
    this.docAbierto.set(false);
  }

  mostrarMatch(nombre: string): void {
    clearTimeout(this.matchTimer);
    this.matchNombre.set(nombre);
    this.matchOn.set(true);
    this.matchTimer = setTimeout(() => this.matchOn.set(false), 2200);
  }

  cerrarMatch(): void {
    this.matchOn.set(false);
  }

  avisarCambio(): void {
    this.cambios.update((n) => n + 1);
  }

  recargarCiclo(): void {
    this.api.cicloActual().subscribe({
      next: (c) => this.ciclo.set(c ?? null),
      error: (e) => {
        this.ciclo.set(null);
        this.error(e);
      },
    });
  }

  reiniciar(): void {
    this.hojaAbierta.set(false);
    this.hoja.set(null);
    this.docAbierto.set(false);
    this.doc.set(null);
    this.matchOn.set(false);
    this.ciclo.set(undefined);
  }
}

export function mensajeDeError(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    const cuerpo = e.error as ApiError | null;
    if (cuerpo?.mensaje) return cuerpo.mensaje;
    if (e.status === 0) return 'No hay conexión con el backend. ¿Está levantado en el puerto 8080?';
    if (e.status === 401) return 'Email o contraseña incorrectos.';
    if (e.status === 403) return 'Tu rol no puede hacer eso.';
  }
  return 'Algo salió mal. Probá de nuevo.';
}
