import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from './core/auth.service';
import { Rol } from './core/models';

interface Enlace {
  ruta: string;
  texto: string;
  icono: string;
  roles?: Rol[];
}

const ENLACES: Enlace[] = [
  { ruta: '/swipe', texto: 'Swipe', icono: '🔥' },
  { ruta: '/resultados', texto: 'Resultados', icono: '📊' },
  { ruta: '/casamientos', texto: 'Casamientos', icono: '💍' },
  { ruta: '/velorios', texto: 'Velorios', icono: '🕯️' },
  { ruta: '/aniversarios', texto: 'Aniversarios', icono: '🎂', roles: ['CALIDAD', 'DIRECCION'] },
  { ruta: '/procedimientos', texto: 'Procedimientos', icono: '📄', roles: ['CALIDAD'] },
  { ruta: '/ciclos', texto: 'Ciclos', icono: '🗓️', roles: ['CALIDAD'] },
];

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule],
  template: `
    @if (auth.usuario(); as u) {
      <header class="barra">
        <a routerLink="/swipe" class="marca">
          <span class="logo">💍</span>
          <span class="nombre">Tinder <small>de procedimientos</small></span>
        </a>
        <span class="espaciador"></span>
        <span class="quien">
          <strong>{{ u.nombre }}</strong>
          <small>{{ u.rol }} · {{ u.areaNombre }}</small>
        </span>
        <button mat-stroked-button class="salir" (click)="auth.logout()">Salir</button>
      </header>
      <nav class="menu">
        @for (e of enlaces(); track e.ruta) {
          <a [routerLink]="e.ruta" routerLinkActive="activo">
            <span aria-hidden="true">{{ e.icono }}</span> {{ e.texto }}
          </a>
        }
      </nav>
    }
    <main>
      <router-outlet />
    </main>
    @if (auth.logueado()) {
      <footer class="lema">Hasta que la burocracia nos separe · Defensoría del Pueblo · ISO 9001</footer>
    }
  `,
  styles: `
    :host { display: flex; flex-direction: column; min-height: 100vh; }
    main { flex: 1; }
    .barra {
      display: flex; align-items: center; gap: 8px;
      background: var(--petroleo); color: #fff;
      padding: 8px 12px; position: sticky; top: 0; z-index: 10;
    }
    .marca { display: flex; align-items: center; gap: 8px; color: #fff; text-decoration: none; }
    .logo { font-size: 1.6rem; }
    .nombre { font-family: var(--fuente-titulos); font-size: 1.25rem; font-weight: 700; line-height: 1; }
    .nombre small { display: block; font-size: .7rem; font-weight: 400; color: var(--ambar); letter-spacing: .05em; }
    .quien { display: flex; flex-direction: column; text-align: right; line-height: 1.1; font-size: .85rem; }
    .quien small { color: #b9cfd3; font-size: .72rem; }
    .barra .salir { color: #fff; --mdc-outlined-button-outline-color: rgba(255,255,255,.5); }
    .menu {
      display: flex; gap: 4px; overflow-x: auto; scrollbar-width: none;
      background: var(--petroleo-claro); padding: 0 8px;
      position: sticky; top: 56px; z-index: 9;
    }
    .menu::-webkit-scrollbar { display: none; }
    .menu a {
      color: #dbe7e9; text-decoration: none; padding: 10px 12px; white-space: nowrap;
      border-bottom: 3px solid transparent; font-size: .95rem;
    }
    .menu a.activo { color: #fff; border-bottom-color: var(--ambar); font-weight: 700; }
    .lema {
      text-align: center; padding: 16px; font-family: var(--fuente-titulos); font-style: italic;
      color: #6b7a7d; font-size: .85rem;
    }
    @media (max-width: 520px) {
      .quien small { display: none; }
      .nombre small { display: none; }
    }
  `,
})
export class AppComponent {
  readonly auth = inject(AuthService);
  readonly enlaces = computed(() => {
    const u = this.auth.usuario();
    return ENLACES.filter((e) => !e.roles || (u && e.roles.includes(u.rol)));
  });
}
