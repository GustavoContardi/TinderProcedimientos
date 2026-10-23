import { Component, input } from '@angular/core';

/** Barra horizontal "me sirve / me sobra" hecha solo con CSS. */
@Component({
  selector: 'app-barra-votos',
  template: `
    @if (pctLike() !== null && pctNope() !== null) {
      <div class="barra" role="img" [attr.aria-label]="pctLike() + '% me sirve, ' + pctNope() + '% me sobra'">
        <div class="sirve" [style.width.%]="pctLike()">
          @if (pctLike()! >= 15) { <span>{{ pctLike() }}%</span> }
        </div>
        <div class="sobra" [style.width.%]="pctNope()">
          @if (pctNope()! >= 15) { <span>{{ pctNope() }}%</span> }
        </div>
      </div>
      <div class="leyenda"><span>♥ me sirve</span><span>me sobra ✕</span></div>
    } @else {
      <div class="barra oculta" title="Pocos votos, por anonimato no mostramos números">
        <span>🔒 sin números</span>
      </div>
    }
  `,
  styles: `
    :host { display: block; min-width: 140px; }
    .barra {
      display: flex; height: 22px; border-radius: 11px; overflow: hidden;
      background: #e6eaeb; font-size: .75rem; font-weight: 700; color: #fff;
    }
    .barra > div { display: flex; align-items: center; justify-content: center; transition: width .8s ease; }
    .sirve { background: var(--match); }
    .sobra { background: var(--rechazo); }
    .oculta { justify-content: center; align-items: center; color: #6b7a7d; background: repeating-linear-gradient(45deg, #eceff0, #eceff0 6px, #e1e6e7 6px, #e1e6e7 12px); }
    .leyenda { display: flex; justify-content: space-between; font-size: .7rem; color: #6b7a7d; margin-top: 2px; }
  `,
})
export class BarraVotosComponent {
  readonly pctLike = input<number | null>(null);
  readonly pctNope = input<number | null>(null);
}
