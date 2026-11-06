import { Component, OnInit, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { avatar } from '../../core/diseno';
import { Rol } from '../../core/models';
import { UiService } from '../../core/ui.service';

interface ItemNav {
  ruta: string;
  texto: string;
  icono: string;
  roles?: Rol[];
}

const NAV: ItemNav[] = [
  { ruta: '/votar', texto: 'Votar', icono: 'swipe' },
  { ruta: '/resultados', texto: 'Resultados', icono: 'leaderboard' },
  { ruta: '/registro', texto: 'Registro', icono: 'menu_book' },
  { ruta: '/gestion', texto: 'Gestión', icono: 'tune', roles: ['CALIDAD'] },
];

/** Cabecera + contenido + barra de navegación inferior. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="cab">
      <div class="anillos" style="--d: 20px; --b: 2.5px"></div>
      <div class="marca"><span class="t">Tinder</span><span class="s">de procedimientos</span></div>
      <div class="flex"></div>
      <span class="ciclo">
        <span class="punto" [style.background]="ui.ciclo() ? '#2E7D73' : '#8A9699'"></span>
        {{ ui.ciclo() === undefined ? '…' : (ui.ciclo()?.nombre ?? 'Sin ciclo') }}
      </span>
      <button class="avatar" (click)="ui.abrirHoja(perfil)" aria-label="Perfil" [style.background]="av().bg" [style.color]="av().color">
        {{ av().ini }}
      </button>
    </header>

    <router-outlet />

    <nav class="nav">
      @for (n of nav(); track n.ruta) {
        <a [routerLink]="n.ruta" routerLinkActive="on">
          <span class="burbuja"><span class="ms">{{ n.icono }}</span></span>
          <span class="txt">{{ n.texto }}</span>
          @if (n.ruta === '/registro' && sinFirmar()) {
            <span class="badge">{{ sinFirmar() }}</span>
          }
        </a>
      }
    </nav>

    <ng-template #perfil>
      @if (auth.usuario(); as u) {
        <div class="perfil">
          <span class="avatar grande" [style.background]="av().bg" [style.color]="av().color">{{ av().ini }}</span>
          <div>
            <div class="nombre">{{ u.nombre }}</div>
            <div class="etq">{{ av().etq }}</div>
          </div>
        </div>
        <div class="anonimato">
          <span class="ms">lock</span>
          Tus votos no quedan asociados a tu nombre. Nadie, ni Calidad ni la Dirección, puede ver qué votaste.
        </div>
        <button class="cambiar" (click)="salir()">Cambiar de usuario</button>
      }
    </ng-template>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .cab { flex: none; display: flex; align-items: center; gap: 10px; padding: 16px 18px 10px; }
    .marca { display: flex; flex-direction: column; line-height: 1; }
    .marca .t { font-family: var(--serif); font-size: 19px; font-weight: 600; color: var(--petroleo); }
    .marca .s { font-size: 10px; letter-spacing: .1em; text-transform: uppercase; color: var(--gris); margin-top: 3px; }
    .ciclo {
      display: inline-flex; align-items: center; gap: 6px; padding: 5px 10px; border-radius: 999px;
      background: var(--papel); border: 1px solid rgba(18,52,59,.09); font-size: 12px; font-weight: 500; color: var(--petroleo);
    }
    .punto { width: 7px; height: 7px; border-radius: 50%; }
    .avatar {
      width: 36px; height: 36px; border-radius: 50%; border: none; font-weight: 600; font-size: 13px;
      cursor: pointer; display: grid; place-items: center; flex: none;
    }
    .avatar.grande { width: 52px; height: 52px; font-size: 17px; cursor: default; }

    .nav { flex: none; display: flex; padding: 6px 8px 18px; background: var(--papel); border-top: 1px solid rgba(18,52,59,.07); }
    .nav a {
      flex: 1; position: relative; display: flex; flex-direction: column; align-items: center; gap: 3px;
      padding: 6px 0; text-decoration: none; color: var(--gris);
    }
    .burbuja { display: grid; place-items: center; width: 56px; height: 30px; border-radius: 15px; transition: background .2s; }
    .burbuja .ms { font-size: 22px; }
    .txt { font-size: 11.5px; font-weight: 500; }
    .nav a.on { color: var(--petroleo); }
    .nav a.on .burbuja { background: #FBE9CC; }
    .nav a.on .ms { font-variation-settings: 'FILL' 1; }
    .nav a.on .txt { font-weight: 600; }
    .badge {
      position: absolute; top: 2px; left: calc(50% + 12px); min-width: 17px; height: 17px; padding: 0 4px;
      border-radius: 9px; background: var(--rechazo); color: #fff; font-size: 10.5px; font-weight: 700; display: grid; place-items: center;
    }

    .perfil { display: flex; align-items: center; gap: 14px; }
    .nombre { font-family: var(--serif); font-size: 22px; color: var(--petroleo); }
    .etq { font-size: 13px; color: var(--gris); }
    .anonimato {
      display: flex; gap: 10px; align-items: flex-start; margin: 18px 0; padding: 14px; border-radius: 14px;
      background: rgba(18,52,59,.05); font-size: 13px; line-height: 1.45; color: var(--texto-2);
    }
    .anonimato .ms { font-size: 18px; color: var(--petroleo); }
    .cambiar {
      width: 100%; height: 48px; border-radius: 14px; border: 1px solid rgba(18,52,59,.2);
      background: transparent; color: var(--petroleo); font-weight: 600; cursor: pointer;
    }
  `,
})
export class ShellComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);
  readonly ui = inject(UiService);

  readonly av = computed(() => {
    const u = this.auth.usuario();
    return avatar(u ?? { nombre: '?', rol: 'EMPLEADO' });
  });
  readonly nav = computed(() => {
    const rol = this.auth.usuario()?.rol;
    return NAV.filter((n) => !n.roles || (rol && n.roles.includes(rol)));
  });
  /** Para la Dirección: necrológicas que esperan su firma. */
  readonly sinFirmar = signal(0);

  constructor() {
    effect(() => {
      this.ui.cambios();
      untracked(() => this.contarSinFirmar());
    });
  }

  ngOnInit(): void {
    this.ui.recargarCiclo();
  }

  private contarSinFirmar(): void {
    if (this.auth.usuario()?.rol !== 'DIRECCION') return;
    this.api.velorios().subscribe({
      next: (v) => this.sinFirmar.set(v.filter((x) => !x.actaFirmada).length),
      error: () => this.sinFirmar.set(0),
    });
  }

  salir(): void {
    this.ui.reiniciar();
    this.auth.logout();
  }
}
