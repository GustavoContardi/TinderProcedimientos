import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Velorio } from '../../core/models';
import { NotificadorService } from '../../core/notificador.service';

/** Necrológica imprimible, fondo oscuro. La Dirección firma el acta desde acá. */
@Component({
  selector: 'app-necrologica',
  imports: [DatePipe, RouterLink, MatButtonModule],
  template: `
    <section class="pagina">
      <div class="no-print acciones">
        <a mat-button routerLink="/velorios">← Volver</a>
        <button mat-stroked-button (click)="imprimir()">🖨️ Imprimir</button>
      </div>

      @if (v(); as v) {
        <article class="necrologica">
          <div class="cruz">✝</div>
          <p class="intro">Con profundo alivio despedimos a</p>
          <h1>{{ v.codigo }} {{ v.nombre }}</h1>
          <p class="fechas">
            ✶ {{ v.fechaNacimiento | date: 'd/M/yyyy' }} &nbsp;—&nbsp; ✝ {{ v.fecha | date: 'd/M/yyyy' }}
          </p>
          <p class="epitafio">{{ v.epitafio }}</p>
          <dl>
            <div><dt>Causa de muerte</dt><dd>{{ v.causaMuerte }}</dd></div>
            <div><dt>Tiempo que nos robaba</dt><dd>{{ v.minutosSemanalesPerdidos }} minutos por semana</dd></div>
            <div><dt>Deudos</dt><dd>{{ v.deudos }}</dd></div>
          </dl>
          <p class="norma">Se conserva como documento obsoleto según ISO 9001 7.5.3 (control de la información documentada). Q.E.P.D.</p>

          <div class="acta">
            @if (v.actaFirmada) {
              <p class="firmada">✔ Acta de defunción firmada por <strong>{{ v.firmadoPorNombre }}</strong>
                el {{ v.fechaFirma | date: "d/M/yyyy 'a las' HH:mm" }}</p>
            } @else {
              <p>Acta de defunción pendiente de firma de la Dirección.</p>
              @if (esDireccion()) {
                <button mat-flat-button class="firmar no-print" (click)="firmar(v)" [disabled]="firmando()">
                  ✍️ Firmar acta de defunción
                </button>
              }
            }
          </div>
        </article>
      }
    </section>
  `,
  styles: `
    .acciones { display: flex; justify-content: space-between; margin-bottom: 12px; }
    .necrologica {
      max-width: 680px; margin: 0 auto; background: #151819; color: #e9e9e9; text-align: center;
      padding: 36px 24px; border-radius: 6px; border: 1px solid #3a3f41;
      box-shadow: inset 0 0 0 6px #151819, inset 0 0 0 7px #5b5f60, 0 14px 40px rgba(0,0,0,.35);
      -webkit-print-color-adjust: exact; print-color-adjust: exact;
    }
    .cruz { font-size: 2.6rem; color: var(--ambar); }
    .intro { font-family: var(--fuente-titulos); font-style: italic; color: #b9bfc1; font-size: 1.1rem; margin: 8px 0 4px; }
    h1 { color: #fff; font-size: 2rem; margin: 0 0 8px; }
    .fechas { color: #b9bfc1; letter-spacing: .06em; }
    .epitafio { font-family: var(--fuente-titulos); font-style: italic; font-size: 1.3rem; line-height: 1.5; margin: 24px auto; max-width: 520px; color: #f4e3c4; }
    dl { max-width: 460px; margin: 0 auto; text-align: left; display: grid; gap: 8px; }
    dl div { border-bottom: 1px solid #33393b; padding-bottom: 6px; }
    dt { font-size: .75rem; text-transform: uppercase; letter-spacing: .08em; color: #8f9a9c; }
    dd { margin: 2px 0 0; }
    .norma { font-size: .78rem; color: #8f9a9c; margin-top: 22px; }
    .acta { margin-top: 20px; border-top: 1px dashed #4b5153; padding-top: 16px; }
    .firmada { color: #9fd3cb; }
    .firmar { --mdc-filled-button-container-color: var(--ambar); --mdc-filled-button-label-text-color: #2b1a00; height: 48px; font-size: 1rem; }
    @media (max-width: 520px) { h1 { font-size: 1.5rem; } .necrologica { padding: 24px 14px; } }
  `,
})
export class NecrologicaComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly notificador = inject(NotificadorService);
  readonly id = input.required<string>();
  readonly v = signal<Velorio | null>(null);
  readonly firmando = signal(false);
  readonly esDireccion = computed(() => this.auth.tieneRol('DIRECCION'));

  ngOnInit(): void {
    this.api.velorio(Number(this.id())).subscribe({ next: (v) => this.v.set(v), error: (e) => this.notificador.error(e) });
  }

  firmar(v: Velorio): void {
    if (!confirm(`¿Firmar el acta de defunción de ${v.codigo}? El procedimiento queda velado para siempre.`)) return;
    this.firmando.set(true);
    this.api.firmarActa(v.id).subscribe({
      next: (firmado) => {
        this.v.set(firmado);
        this.notificador.ok(`${v.codigo} descansa en paz. Acta firmada.`);
        this.firmando.set(false);
      },
      error: (e) => {
        this.notificador.error(e);
        this.firmando.set(false);
      },
    });
  }

  imprimir(): void {
    window.print();
  }
}
