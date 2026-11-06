import { Component, OnDestroy, computed, effect, inject, signal, untracked } from '@angular/core';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { ESTADOS_CASAMIENTO, fCorta, fFirma, fLarga, lista } from '../core/diseno';
import { Casamiento, Velorio } from '../core/models';
import { UiService } from '../core/ui.service';

const MS_FIRMA = 1300;

/** Panel que entra desde la derecha con la libreta de casamiento o la necrológica (imprimibles). */
@Component({
  selector: 'app-documento',
  template: `
    <div class="panel" [class.on]="ui.docAbierto()" [class.oscuro]="esNecro()">
      <div class="barra">
        <button class="redondo" (click)="ui.cerrarDoc()" aria-label="Volver"><span class="ms">arrow_back</span></button>
        <span class="flex titulo">{{ esNecro() ? 'Necrológica' : 'Libreta de casamiento' }}</span>
        <button class="imprimir" (click)="imprimir()"><span class="ms">print</span>Imprimir</button>
      </div>
      <div class="contenido">
        @if (!esNecro() && casamiento(); as c) {
          <div class="libreta" data-print="1">
            <div class="anillos" style="--d: 34px; --b: 2.5px; --c1: #8A6D3B; --c2: #8A6D3B; margin: 0 auto"></div>
            <div class="sup">Registro Civil de Procedimientos · Defensoría del Pueblo</div>
            <h1>Libreta de casamiento</h1>
            <div class="acta">Acta N.º {{ acta(c.id) }} · Tomo ISO 9001</div>
            <p class="cuerpo">
              En la fecha <strong>{{ fLarga(c.fecha) }}</strong>, ante el oficiante <strong>{{ c.oficianteNombre }}</strong>,
              el procedimiento <strong>{{ c.codigo }} «{{ c.nombre }}»</strong> contrae matrimonio{{ c.grupal ? ' grupal' : '' }}
              con <strong>{{ lista(c.areas) }}</strong>, quienes se comprometen públicamente a usarlo en las buenas,
              en las malas y en las auditorías.
            </p>
            <div class="seccion">Votos</div>
            <p class="votos">«{{ c.votos }}»</p>
            <div class="seccion">Acuerdo prenupcial · versión express</div>
            <div class="express">{{ c.versionExpress || '—' }}</div>
            <div class="datos">
              <div><strong>Padrino</strong><span>{{ c.padrinoNombre ?? 'Sin padrino' }}</span></div>
              <div><strong>Aniversario (revisión)</strong><span>{{ fCorta(c.fechaAniversario) }}</span></div>
              <div><strong>Estado</strong><span>{{ estadoCasamiento(c) }}</span></div>
            </div>
            <p class="lema">«Hasta que la burocracia nos separe»</p>
            <div class="firmas"><div>Oficiante</div><div>Padrino</div><div>Calidad</div></div>
          </div>
        }

        @if (esNecro() && velorio(); as v) {
          <div class="necro" data-print="1">
            <div class="cruz">✝</div>
            <p class="intro">Con profundo alivio despedimos a</p>
            <h1>{{ v.codigo }} {{ v.nombre }}</h1>
            <p class="fechas">✶ {{ fCorta(v.fechaNacimiento) }} &nbsp;—&nbsp; ✝ {{ fCorta(v.fecha) }}</p>
            <p class="epitafio">{{ v.epitafio }}</p>
            <div class="datos-necro">
              <div><div class="k">Causa de muerte</div><div class="v">{{ v.causaMuerte }}</div></div>
              <div><div class="k">Tiempo que nos robaba</div><div class="v">{{ v.minutosSemanalesPerdidos }} minutos por semana</div></div>
              <div><div class="k">Deudos</div><div class="v">{{ v.deudos }}</div></div>
            </div>
            <p class="norma">
              Se conserva como documento obsoleto según ISO 9001 7.5.3 (control de la información documentada). Q.E.P.D.
            </p>
            <div class="pie">
              @if (v.actaFirmada) {
                <p class="firmada">
                  ✔ Acta de defunción firmada por <strong>{{ v.firmadoPorNombre }}</strong>
                  @if (v.fechaFirma) { el {{ fFirma(v.fechaFirma) }} }
                </p>
              } @else {
                <p class="pendiente">Acta de defunción pendiente de firma de la Dirección.</p>
                @if (esDireccion()) {
                  <button class="firmar" (pointerdown)="apretar()" (pointerup)="soltar()" (pointerleave)="soltar()"
                          (pointercancel)="soltar()" (contextmenu)="$event.preventDefault()" [disabled]="firmando()">
                    <span class="relleno" [style.width.%]="progreso() * 100"></span>
                    <span class="etiqueta" [style.color]="progreso() > .5 ? '#3D2600' : '#F2A541'">
                      <span class="ms">draw</span>{{ firmando() ? 'Firmando…' : 'Mantené apretado para firmar' }}
                    </span>
                  </button>
                  <p class="aviso">Firmar es definitivo: el procedimiento queda velado para siempre.</p>
                }
              }
            </div>
          </div>
        }
      </div>
    </div>
  `,
  styles: `
    .panel {
      position: absolute; inset: 0; z-index: 50; display: flex; flex-direction: column;
      background: #EDE6D6; color: #4A3A1C; --boton: rgba(74,58,28,.08);
      transform: translateX(102%); visibility: hidden;
      transition: transform .4s cubic-bezier(.2,.8,.25,1), visibility 0s .4s;
    }
    .panel.on { transform: translateX(0); visibility: visible; transition: transform .4s cubic-bezier(.2,.8,.25,1); }
    .panel.oscuro { background: #0E1214; color: #E9E9E9; --boton: rgba(255,255,255,.08); }
    .barra { flex: none; display: flex; align-items: center; gap: 8px; padding: 16px 14px 10px; }
    .titulo { font-weight: 600; font-size: 15px; }
    .redondo, .imprimir { border: none; background: var(--boton); color: inherit; cursor: pointer; }
    .redondo { width: 40px; height: 40px; border-radius: 50%; display: grid; place-items: center; }
    .redondo .ms { font-size: 22px; }
    .imprimir { height: 40px; padding: 0 14px 0 10px; border-radius: 999px; display: flex; align-items: center; gap: 6px; font-weight: 600; font-size: 13.5px; }
    .imprimir .ms { font-size: 19px; }
    .contenido { flex: 1; overflow-y: auto; padding: 8px 16px 30px; }

    .libreta {
      background-color: #FFFAF0;
      background-image: repeating-linear-gradient(0deg, transparent 0 27px, rgba(138,109,59,.07) 27px 28px);
      color: #2B2B2B; border: 3px double #8A6D3B;
      box-shadow: 0 0 0 6px #FFFAF0, 0 0 0 7px #C9B28A, 0 14px 34px rgba(74,58,28,.18);
      margin: 8px; padding: 28px 20px 30px; font-family: var(--serif); text-align: center;
      -webkit-print-color-adjust: exact; print-color-adjust: exact;
    }
    .sup { margin-top: 12px; font-family: var(--sans); font-size: 10px; font-weight: 600; letter-spacing: .16em; text-transform: uppercase; color: #7A5E2E; }
    .libreta h1 { margin: 10px 0 4px; font-size: 30px; font-weight: 500; font-variant: small-caps; letter-spacing: .02em; color: #4A3A1C; }
    .acta { font-size: 13px; color: #7A5E2E; }
    .cuerpo { margin: 20px 0 0; text-align: justify; font-size: 15.5px; line-height: 1.65; hyphens: auto; }
    .seccion { margin: 22px 0 6px; font-family: var(--sans); font-size: 11px; font-weight: 600; letter-spacing: .14em; text-transform: uppercase; color: #4A3A1C; }
    .votos { margin: 0; font-style: italic; font-size: 17px; line-height: 1.5; }
    .express { text-align: left; white-space: pre-line; font-family: var(--sans); font-size: 13.5px; line-height: 1.55; background: #fff; border: 1px dashed #C9B28A; padding: 12px 14px; }
    .datos { margin: 20px 0 0; text-align: left; font-size: 14.5px; }
    .datos > div { display: flex; justify-content: space-between; gap: 12px; padding: 6px 0; border-bottom: 1px dotted #C9B28A; }
    .datos span { text-align: right; }
    .lema { margin: 24px 0 0; font-style: italic; font-size: 21px; color: var(--petroleo); }
    .firmas { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 40px; font-family: var(--sans); font-size: 11px; color: #4A3A1C; }
    .firmas > div { border-top: 1px solid #4A3A1C; padding-top: 5px; }

    .necro {
      background: #151819; color: #E9E9E9; text-align: center; padding: 34px 20px 26px; border-radius: 6px;
      border: 1px solid #3A3F41; box-shadow: inset 0 0 0 6px #151819, inset 0 0 0 7px #5B5F60, 0 14px 40px rgba(0,0,0,.35);
      -webkit-print-color-adjust: exact; print-color-adjust: exact;
    }
    .cruz { font-size: 34px; color: var(--ambar); line-height: 1; }
    .intro { margin: 14px 0 6px; font-family: var(--serif); font-style: italic; font-size: 18px; color: #B9BFC1; }
    .necro h1 { margin: 0; font-family: var(--serif); font-weight: 500; font-size: 30px; line-height: 1.1; color: #fff; text-wrap: balance; }
    .fechas { margin: 10px 0 0; font-size: 13px; letter-spacing: .08em; color: #B9BFC1; }
    .epitafio { margin: 24px auto; max-width: 300px; font-family: var(--serif); font-style: italic; font-size: 21px; line-height: 1.45; color: #F4E3C4; }
    .datos-necro { text-align: left; display: flex; flex-direction: column; gap: 10px; }
    .datos-necro > div { border-bottom: 1px solid #33393B; padding-bottom: 8px; }
    .k { font-size: 10.5px; letter-spacing: .1em; text-transform: uppercase; color: #9AA3A5; }
    .v { margin-top: 3px; font-size: 15px; }
    .norma { margin: 20px 0 0; font-size: 11.5px; line-height: 1.5; color: #9AA3A5; }
    .pie { margin-top: 20px; padding-top: 18px; border-top: 1px dashed #4B5153; }
    .firmada { margin: 0; font-size: 14px; line-height: 1.5; color: #9FD3CB; }
    .pendiente { margin: 0 0 14px; font-size: 14px; color: #B9BFC1; }
    .firmar {
      position: relative; overflow: hidden; width: 100%; height: 56px; border-radius: 14px;
      border: 1.5px solid var(--ambar); background: transparent; color: var(--ambar);
      font-weight: 600; font-size: 15px; cursor: pointer; touch-action: none; user-select: none; -webkit-user-select: none;
    }
    .firmar .relleno { position: absolute; left: 0; top: 0; bottom: 0; background: var(--ambar); }
    .firmar .etiqueta { position: relative; display: flex; align-items: center; justify-content: center; gap: 8px; }
    .firmar .ms { font-size: 20px; }
    .aviso { margin: 10px 0 0; font-size: 12px; color: #9AA3A5; }
  `,
})
export class DocumentoComponent implements OnDestroy {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  readonly ui = inject(UiService);

  readonly casamiento = signal<Casamiento | null>(null);
  readonly velorio = signal<Velorio | null>(null);
  readonly progreso = signal(0);
  readonly firmando = signal(false);
  readonly esNecro = computed(() => this.ui.doc()?.tipo === 'necro');
  readonly esDireccion = computed(() => this.auth.usuario()?.rol === 'DIRECCION');
  private raf = 0;

  readonly fCorta = fCorta;
  readonly fLarga = fLarga;
  readonly fFirma = fFirma;
  readonly lista = lista;

  constructor() {
    effect(() => {
      const d = this.ui.doc();
      if (!d) return;
      untracked(() => this.cargar(d.tipo, d.id));
    });
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.raf);
  }

  private cargar(tipo: 'libreta' | 'necro', id: number): void {
    this.progreso.set(0);
    if (tipo === 'libreta') {
      if (this.casamiento()?.id !== id) this.casamiento.set(null);
      this.api.casamiento(id).subscribe({ next: (c) => this.casamiento.set(c), error: (e) => this.ui.error(e) });
    } else {
      if (this.velorio()?.id !== id) this.velorio.set(null);
      this.api.velorio(id).subscribe({ next: (v) => this.velorio.set(v), error: (e) => this.ui.error(e) });
    }
  }

  acta(id: number): string {
    return String(id).padStart(4, '0');
  }

  estadoCasamiento(c: Casamiento): string {
    return ESTADOS_CASAMIENTO[c.estado][0];
  }

  /** Mantener apretado ~1,3 s para firmar el acta. */
  apretar(): void {
    if (this.firmando()) return;
    cancelAnimationFrame(this.raf);
    const t0 = performance.now();
    const paso = () => {
      const k = Math.min(1, (performance.now() - t0) / MS_FIRMA);
      this.progreso.set(k);
      if (k >= 1) this.firmar();
      else this.raf = requestAnimationFrame(paso);
    };
    this.raf = requestAnimationFrame(paso);
  }

  soltar(): void {
    cancelAnimationFrame(this.raf);
    if (this.progreso() < 1) this.progreso.set(0);
  }

  private firmar(): void {
    const v = this.velorio();
    if (!v || this.firmando()) return;
    this.firmando.set(true);
    this.api.firmarActa(v.id).subscribe({
      next: (firmado) => {
        this.velorio.set(firmado);
        this.firmando.set(false);
        this.progreso.set(0);
        this.ui.ok(`${v.codigo} descansa en paz. Acta firmada.`);
        this.ui.avisarCambio();
      },
      error: (e) => {
        this.firmando.set(false);
        this.progreso.set(0);
        this.ui.error(e);
      },
    });
  }

  imprimir(): void {
    window.print();
  }
}
