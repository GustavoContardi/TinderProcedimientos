import { Component, HostListener, inject } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { UiService } from './core/ui.service';
import { DocumentoComponent } from './shared/documento.component';

/**
 * Escenario + marco de teléfono (en pantallas ≥ 520px) y todas las capas flotantes:
 * overlay de match, hoja inferior, documento (libreta / necrológica) y toast.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NgTemplateOutlet, DocumentoComponent],
  template: `
    <div class="escenario">
      <div class="marco">
        <router-outlet />

        <div class="match" [class.on]="ui.matchOn()" (click)="ui.cerrarMatch()" role="status" aria-live="polite">
          <div class="anillos grande"></div>
          <div class="match-titulo">¡Es un match!</div>
          <div class="match-sub">Preparen la libreta</div>
          <div class="match-txt">
            A tu área también le sirve {{ ui.matchNombre() }}. Si se confirma al cierre del trimestre, hay casamiento.
          </div>
        </div>

        <div class="velo" [class.on]="ui.hojaAbierta()" (click)="ui.cerrarHoja()"></div>
        <div class="hoja" [class.on]="ui.hojaAbierta()" role="dialog" aria-modal="true">
          <div class="manija"></div>
          @if (ui.hoja(); as contenido) {
            <ng-container [ngTemplateOutlet]="contenido" />
          }
        </div>

        <app-documento />

        <div class="toast" [class.on]="ui.toastOn()" [class.con-nav]="auth.logueado()" [class.error]="ui.toastError()">
          <span class="ms">{{ ui.toastError() ? 'error' : 'check_circle' }}</span>{{ ui.toastTxt() }}
        </div>
      </div>
    </div>
  `,
  styles: `
    .escenario {
      min-height: 100vh; display: flex; align-items: center; justify-content: center;
      background: var(--escenario); padding: 32px 16px;
    }
    .marco {
      position: relative; width: 390px; height: min(844px, calc(100dvh - 64px)); min-height: 640px;
      border-radius: 44px; overflow: hidden; overflow: clip; background: var(--crema);
      box-shadow: 0 0 0 1px rgba(18,52,59,.1), 0 40px 80px -20px rgba(18,52,59,.35);
      display: flex; flex-direction: column;
    }
    @media (max-width: 519px) {
      .escenario { padding: 0; background: var(--crema); }
      .marco { width: 100vw; height: 100dvh; min-height: 0; border-radius: 0; box-shadow: none; }
    }

    .match {
      position: absolute; inset: 0; z-index: 60; background: rgba(18,52,59,.96);
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      gap: 18px; text-align: center; padding: 30px;
      opacity: 0; pointer-events: none; transition: opacity .3s;
    }
    .match.on { opacity: 1; pointer-events: auto; }
    .anillos.grande {
      --d: 84px; --b: 5px; --c2: #6FD0C2;
      transform: scale(.4); transition: transform .55s cubic-bezier(.3,1.6,.5,1);
    }
    .match.on .anillos.grande { transform: scale(1); }
    .match-titulo { font-family: var(--serif); font-style: italic; font-size: 48px; line-height: 1; color: var(--crema); letter-spacing: -.02em; }
    .match-sub { font-size: 13px; font-weight: 600; letter-spacing: .18em; text-transform: uppercase; color: var(--ambar); }
    .match-txt { font-size: 13.5px; line-height: 1.5; color: rgba(245,240,230,.7); max-width: 260px; }

    .velo {
      position: absolute; inset: 0; z-index: 40; background: rgba(10,25,28,.42);
      opacity: 0; pointer-events: none; transition: opacity .25s;
    }
    .velo.on { opacity: 1; pointer-events: auto; }
    .hoja {
      position: absolute; left: 0; right: 0; bottom: 0; z-index: 41; max-height: 90%; overflow-y: auto; visibility: hidden;
      transition: transform .38s cubic-bezier(.2,.8,.25,1), visibility 0s .38s;
      background: var(--papel); border-radius: 26px 26px 0 0; padding: 10px 20px 26px;
      box-shadow: 0 -10px 40px rgba(18,52,59,.18);
      transform: translateY(105%);
    }
    .hoja.on { transform: translateY(0); visibility: visible; transition: transform .38s cubic-bezier(.2,.8,.25,1); }
    .manija { width: 40px; height: 4px; border-radius: 2px; background: rgba(18,52,59,.18); margin: 0 auto 16px; }

    .toast {
      position: absolute; left: 50%; bottom: 24px; z-index: 70;
      transform: translate(-50%, 12px); opacity: 0; transition: opacity .25s, transform .3s; pointer-events: none;
      width: max-content; max-width: 88%; display: flex; align-items: center; gap: 10px;
      padding: 12px 16px; border-radius: 14px; background: var(--petroleo); color: var(--crema);
      font-size: 13.5px; line-height: 1.35; box-shadow: 0 10px 30px rgba(18,52,59,.3);
    }
    .toast.con-nav { bottom: 92px; }
    .toast.on { opacity: 1; transform: translate(-50%, 0); }
    .toast .ms { font-size: 18px; color: var(--ambar); }
    .toast.error .ms { color: #F4A08E; }
  `,
})
export class AppComponent {
  readonly ui = inject(UiService);
  readonly auth = inject(AuthService);

  @HostListener('window:keydown.escape')
  escape(): void {
    if (this.ui.hojaAbierta()) this.ui.cerrarHoja();
    else if (this.ui.docAbierto()) this.ui.cerrarDoc();
    else this.ui.cerrarMatch();
  }
}
