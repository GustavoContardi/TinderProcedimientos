import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, tap, throwError } from 'rxjs';
import { Rol, Usuario } from './models';

const CLAVE_CREDENCIALES = 'tinder.credenciales';
const CLAVE_USUARIO = 'tinder.usuario';

/** Login con HTTP Basic. Las credenciales viven en sessionStorage (se borran al cerrar la pestaña). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly usuario = signal<Usuario | null>(leerUsuario());
  readonly logueado = computed(() => this.usuario() !== null);

  credenciales(): string | null {
    return sessionStorage.getItem(CLAVE_CREDENCIALES);
  }

  login(email: string, password: string): Observable<Usuario> {
    sessionStorage.setItem(CLAVE_CREDENCIALES, aBase64(`${email.trim()}:${password}`));
    return this.http.get<Usuario>('/api/auth/me').pipe(
      tap((u) => {
        sessionStorage.setItem(CLAVE_USUARIO, JSON.stringify(u));
        this.usuario.set(u);
      }),
      catchError((e) => {
        this.limpiar();
        return throwError(() => e);
      }),
    );
  }

  logout(): void {
    this.limpiar();
    this.router.navigate(['/login']);
  }

  tieneRol(...roles: Rol[]): boolean {
    const u = this.usuario();
    return !!u && roles.includes(u.rol);
  }

  private limpiar(): void {
    sessionStorage.removeItem(CLAVE_CREDENCIALES);
    sessionStorage.removeItem(CLAVE_USUARIO);
    this.usuario.set(null);
  }
}

function leerUsuario(): Usuario | null {
  try {
    const raw = sessionStorage.getItem(CLAVE_USUARIO);
    return raw && sessionStorage.getItem(CLAVE_CREDENCIALES) ? (JSON.parse(raw) as Usuario) : null;
  } catch {
    return null;
  }
}

function aBase64(texto: string): string {
  const bytes = new TextEncoder().encode(texto);
  let binario = '';
  bytes.forEach((b) => (binario += String.fromCharCode(b)));
  return btoa(binario);
}
