import { Component, OnInit, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../core/api.service';
import { Casamiento } from '../../core/models';
import { NotificadorService } from '../../core/notificador.service';

/** Vista imprimible con estética de acta del Registro Civil. */
@Component({
  selector: 'app-libreta',
  imports: [DatePipe, RouterLink, MatButtonModule],
  template: `
    <section class="pagina">
      <div class="no-print acciones">
        <a mat-button routerLink="/casamientos">← Volver</a>
        <button mat-flat-button (click)="imprimir()">🖨️ Imprimir libreta</button>
      </div>

      @if (c(); as c) {
        <article class="libreta">
          <div class="escudo">⚖️</div>
          <p class="organismo">Registro Civil de Procedimientos · Defensoría del Pueblo</p>
          <h1>Libreta de casamiento</h1>
          <p class="acta">Acta N.º {{ c.id.toString().padStart(4, '0') }} · Tomo ISO 9001</p>

          <p class="texto">
            En la fecha <strong>{{ c.fecha | date: "d 'de' MMMM 'de' yyyy" }}</strong>, ante el oficiante
            <strong>{{ c.oficianteNombre }}</strong>, el procedimiento
            <strong>{{ c.codigo }} «{{ c.nombre }}»</strong> contrae matrimonio
            @if (c.grupal) { <em>grupal</em> } con
            <strong>{{ c.areas.join(', ') }}</strong>, quienes se comprometen públicamente a usarlo
            en las buenas, en las malas y en las auditorías.
          </p>

          <h2>Votos</h2>
          <blockquote>{{ c.votos }}</blockquote>

          <h2>Acuerdo prenupcial (versión express)</h2>
          <blockquote class="express">{{ c.versionExpress ?? '—' }}</blockquote>

          <dl>
            <div><dt>Padrino</dt><dd>{{ c.padrinoNombre ?? 'Sin padrino' }}</dd></div>
            <div><dt>Primer aniversario (revisión)</dt><dd>{{ c.fechaAniversario | date: 'd/M/yyyy' }}</dd></div>
            <div><dt>Estado</dt><dd>{{ c.estado }}</dd></div>
          </dl>

          <p class="lema">«Hasta que la burocracia nos separe»</p>

          <div class="firmas">
            <div><span></span>Oficiante</div>
            <div><span></span>Padrino</div>
            <div><span></span>Calidad</div>
          </div>
        </article>
      }
    </section>
  `,
  styles: `
    .acciones { display: flex; justify-content: space-between; margin-bottom: 12px; }
    .libreta {
      max-width: 720px; margin: 0 auto; background: #fffaf0; color: #2b2b2b;
      border: 3px double #8a6d3b; outline: 8px solid #fffaf0; box-shadow: 0 0 0 10px #c9b28a, 0 10px 30px rgba(0,0,0,.15);
      padding: 32px 28px; font-family: var(--fuente-titulos); text-align: center;
      background-image: repeating-linear-gradient(0deg, transparent, transparent 31px, rgba(138,109,59,.07) 32px);
    }
    .escudo { font-size: 2.6rem; }
    .organismo { text-transform: uppercase; letter-spacing: .12em; font-size: .75rem; color: #8a6d3b; margin: 4px 0; }
    h1 { font-size: 2rem; color: #4a3a1c; margin: 6px 0; font-variant: small-caps; }
    .acta { font-size: .85rem; color: #8a6d3b; margin: 0 0 18px; }
    .texto { text-align: justify; line-height: 1.6; font-size: 1.05rem; }
    h2 { font-size: 1.05rem; color: #4a3a1c; margin: 18px 0 6px; text-transform: uppercase; letter-spacing: .08em; }
    blockquote { font-style: italic; margin: 0 auto; max-width: 560px; line-height: 1.5; }
    .express { font-style: normal; font-family: var(--fuente-cuerpo); background: #fff; border: 1px dashed #c9b28a; padding: 10px; text-align: left; white-space: pre-line; }
    dl { display: grid; gap: 6px; margin: 20px auto; max-width: 420px; text-align: left; }
    dl div { display: flex; justify-content: space-between; border-bottom: 1px dotted #c9b28a; }
    dt { font-weight: 700; }
    dd { margin: 0; }
    .lema { font-size: 1.35rem; font-style: italic; color: var(--petroleo); margin: 22px 0; }
    .firmas { display: flex; justify-content: space-around; gap: 12px; margin-top: 36px; font-size: .8rem; }
    .firmas div { display: flex; flex-direction: column; align-items: center; gap: 4px; flex: 1; }
    .firmas span { display: block; width: 100%; max-width: 160px; border-top: 1px solid #4a3a1c; }
    @media (max-width: 520px) { .libreta { padding: 20px 14px; } h1 { font-size: 1.5rem; } }
  `,
})
export class LibretaComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notificador = inject(NotificadorService);
  readonly id = input.required<string>();
  readonly c = signal<Casamiento | null>(null);

  ngOnInit(): void {
    this.api.casamiento(Number(this.id())).subscribe({ next: (c) => this.c.set(c), error: (e) => this.notificador.error(e) });
  }

  imprimir(): void {
    window.print();
  }
}
