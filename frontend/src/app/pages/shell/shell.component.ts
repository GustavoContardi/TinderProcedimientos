import { Component, OnInit, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { avatar } from '../../core/diseno';
import { Rol } from '../../core/models';
import { UiService } from '../../core/ui.service';

interface ItemNav {
  ruta: string;
  texto: string;
  icono: string;
  /** Rutas que dejan prendido este ítem. */
  prefijos: string[];
  roles?: Rol[];
}

const NAV: ItemNav[] = [
  { ruta: '/swipe', texto: 'Swipe', icono: 'swipe', prefijos: ['/swipe'] },
  { ruta: '/resultados', texto: 'Resultados', icono: 'leaderboard', prefijos: ['/resultados'] },
  { ruta: '/casamientos', texto: 'Registro', icono: 'menu_book', prefijos: ['/casamientos', '/velorios', '/aniversarios', '/archivo'] },
  { ruta: '/tablero', texto: 'Tablero', icono: 'view_kanban', prefijos: ['/tablero'] },
  {
    ruta: '/juez-de-paz',
    texto: 'Juez de paz',
    icono: 'gavel',
    prefijos: ['/juez-de-paz', '/matriz', '/objeciones', '/segundas-citas', '/gestion'],
    roles: ['JUEZ_PAZ', 'DIRECCION'],
  },
];

const ROLES: Record<Rol, string> = { EMPLEADO: 'Empleado/a', JUEZ_PAZ: 'Juez de paz', DIRECCION: 'Dirección' };

/** Cabecera + contenido + barra de navegación inferior. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink],
  template: `
    <header class="cab">
      <div class="anillos" style="--d: 20px; --b: 2.5px"></div>
      <div class="marca"><span class="t">Pasá y Rebotá</span><span class="s">Procedimientos</span></div>
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
        <a [routerLink]="n.ruta" [class.on]="activo(n)">
          <span class="burbuja"><span class="ms">{{ n.icono }}</span></span>
          <span class="txt">{{ n.texto }}</span>
          @if (n.ruta === '/casamientos' && sinFirmar()) {
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
        <div class="rol"><span class="ms">badge</span>Rol: <strong>{{ roles[u.rol] }}</strong></div>
        <div class="anonimato">
          <span class="ms">lock</span>
          Tus votos y objeciones no quedan asociados a tu nombre. Nadie, ni el juez de paz ni la Dirección, puede ver qué votaste.
        </div>
        <button class="opcion" (click)="ir('/mis-procedimientos')"><span class="ms">edit_note</span>Procedimientos de los que soy dueño/a</button>
        <button class="cambiar" (click)="salir()">Cambiar de usuario</button>
      }
    </ng-template>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .cab { flex: none; display: flex; align-items: center; gap: 10px; padding: 16px 18px 10px; }
    .marca { display: flex; flex-direction: column; line-height: 1; }
    .marca .t { font-family: var(--serif); font-size: 19px; white-space: nowrap; font-weight: 600; color: var(--petroleo); }
    .marca .s { font-size: 10px; letter-spacing: .1em; text-transform: uppercase; color: var(--gris); margin-top: 3px; }
    .ciclo {
      flex: none; white-space: nowrap; display: inline-flex; align-items: center; gap: 6px; padding: 5px 10px; border-radius: 999px;
      background: var(--papel); border: 1px solid rgba(18,52,59,.09); font-size: 12px; font-weight: 500; color: var(--petroleo);
    }
    .punto { width: 7px; height: 7px; border-radius: 50%; }
    .avatar {
      width: 36px; height: 36px; border-radius: 50%; border: none; font-weight: 600; font-size: 13px;
      cursor: pointer; display: grid; place-items: center; flex: none;
    }
    .avatar.grande { width: 52px; height: 52px; font-size: 17px; cursor: default; }

    .nav { flex: none; display: flex; padding: 6px 4px 18px; background: var(--papel); border-top: 1px solid rgba(18,52,59,.07); }
    .nav a {
      flex: 1; min-width: 0; position: relative; display: flex; flex-direction: column; align-items: center; gap: 3px;
      padding: 6px 0; text-decoration: none; color: var(--gris);
    }
    .burbuja { display: grid; place-items: center; width: 52px; height: 30px; border-radius: 15px; transition: background .2s; }
    .burbuja .ms { font-size: 22px; }
    .txt { font-size: 11px; font-weight: 500; white-space: nowrap; }
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
    .rol { display: flex; align-items: center; gap: 8px; margin-top: 16px; font-size: 13.5px; color: var(--texto-2); }
    .rol .ms { font-size: 18px; color: var(--petroleo); }
    .rol strong { color: var(--petroleo); }
    .anonimato {
      display: flex; gap: 10px; align-items: flex-start; margin: 14px 0; padding: 14px; border-radius: 14px;
      background: rgba(18,52,59,.05); font-size: 13px; line-height: 1.45; color: var(--texto-2);
    }
    .anonimato .ms { font-size: 18px; color: var(--petroleo); }
    .opcion {
      width: 100%; display: flex; align-items: center; gap: 10px; padding: 12px 14px; margin-bottom: 10px; border-radius: 14px;
      border: 1px solid rgba(18,52,59,.12); background: #fff; color: var(--petroleo); font-weight: 600; cursor: pointer; text-align: left;
    }
    .opcion .ms { font-size: 20px; }
    .cambiar {
      width: 100%; height: 48px; border-radius: 14px; border: 1px solid rgba(18,52,59,.2);
      background: transparent; color: var(--petroleo); font-weight: 600; cursor: pointer;
    }
  `,
})
export class ShellComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);
  readonly ui = inject(UiService);
  readonly roles = ROLES;

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map((e) => (e as NavigationEnd).urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  readonly av = computed(() => {
    const u = this.auth.usuario();
    return avatar(u ?? { nombre: '?', rol: 'EMPLEADO' });
  });
  readonly nav = computed(() => {
    const rol = this.auth.usuario()?.rol;
    return NAV.filter((n) => !n.roles || (rol && n.roles.includes(rol)));
  });
  /** Actas de defunción que esperan MI firma (como juez, Dirección o dueño). */
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

  activo(n: ItemNav): boolean {
    const u = this.url();
    return n.prefijos.some((p) => u === p || u.startsWith(p + '/') || u.startsWith(p + '?'));
  }

  private contarSinFirmar(): void {
    const u = this.auth.usuario();
    if (!u) return;
    this.api.velorios().subscribe({
      next: (vs) =>
        this.sinFirmar.set(
          vs.filter(
            (v) =>
              !v.completa &&
              !v.firmas.some((f) => f.usuarioId === u.id) &&
              v.firmas.some(
                (f) =>
                  !f.usuarioId &&
                  ((f.rol === 'JUEZ_PAZ' && u.rol === 'JUEZ_PAZ') ||
                    (f.rol === 'DIRECCION' && u.rol === 'DIRECCION') ||
                    (f.rol === 'DUENO' && v.duenoId === u.id)),
              ),
          ).length,
        ),
      error: () => this.sinFirmar.set(0),
    });
  }

  ir(ruta: string): void {
    this.ui.cerrarHoja();
    this.router.navigate([ruta]);
  }

  salir(): void {
    this.ui.reiniciar();
    this.auth.logout();
  }
}
