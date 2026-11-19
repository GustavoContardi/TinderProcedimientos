import { Component, OnDestroy, computed, effect, inject, signal, untracked } from '@angular/core';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { CATEGORIAS, ESTADOS_CASAMIENTO, fCorta, fFirma, fLarga, lista } from '../core/diseno';
import { Casamiento, CategoriaObjecion, Velorio } from '../core/models';
import { UiService } from '../core/ui.service';

const MS_FIRMA = 1300;

/** Panel que entra desde la derecha con la libreta de casamiento o la necrológica (imprimibles). */
@Component({
  selector: 'app-documento',
  template: `
    <div class="panel" [class.on]="ui.docAbierto()" [class.oscuro]="esNecro()">
      <div class="barra">
        <button class="redondo" (click)="ui.cerrarDoc()" aria-label="Volver"><span class="ms">arrow_back</span></button>
        <span class="flex titulo">{{ esNecro() ? 'Acta de defunción' : 'Libreta de casamiento' }}</span>
        <button class="imprimir" (click)="imprimir()"><span class="ms">print</span>Imprimir</button>
      </div>
      <div class="contenido">
        @if (!esNecro() && casamiento(); as c) {
          @if (c.estado === 'POSTERGADO') {
            <div class="postergado"><span class="ms">pause_circle</span>Boda postergada: una objeción fundada la frenó y el procedimiento volvió a segunda cita.</div>
          }
          <div class="libreta" data-print="1">
            <div class="anillos" style="--d: 34px; --b: 2.5px; --c1: #8A6D3B; --c2: #8A6D3B; margin: 0 auto"></div>
            <div class="sup">Registro Civil de Procedimientos · Defensoría del Pueblo</div>
            <h1>Libreta de casamiento</h1>
            <div class="acta">Libreta N.º {{ c.numero }} · Tomo ISO 9001</div>
            <p class="cuerpo">
              En la fecha <strong>{{ fLarga(c.fecha) }}</strong>, ante el juez de paz <strong>{{ c.oficianteNombre }}</strong>,
              el procedimiento <strong>{{ c.codigo }} «{{ c.nombre }}»</strong>, versión {{ c.versionProcedimiento }}, contrae
              matrimonio{{ c.grupal ? ' grupal' : '' }} con <strong>{{ lista(c.areas) }}</strong>, quienes se comprometen
              públicamente a usarlo en las buenas, en las malas y en las auditorías.
            </p>
            @if (c.resultados.length) {
              <div class="seccion">Aprobación por área</div>
              <div class="pcts">
                @for (r of c.resultados; track r.areaNombre) {
                  <div class="pct"><span class="n">{{ r.pct }} %</span><span class="a">{{ r.areaNombre }}</span></div>
                }
              </div>
            }
            <div class="seccion">Votos</div>
            <p class="votos">«{{ c.votos }}»</p>
            <div class="seccion">Acuerdo prenupcial · versión express</div>
            <div class="express">{{ c.versionExpress || '—' }}</div>
            @if (c.condiciones) {
              <div class="seccion">Condiciones</div>
              <div class="express">{{ c.condiciones }}</div>
            }
            <div class="datos">
              <div><strong>Dueño</strong><span>{{ c.duenoNombre ?? 'Sin dueño' }}</span></div>
              <div><strong>Padrino</strong><span>{{ c.padrinoNombre ?? 'Sin padrino' }}</span></div>
              <div><strong>Testigos</strong><span>{{ testigos(c) }}</span></div>
              <div><strong>Aniversario (revisión)</strong><span>{{ fCorta(c.fechaAniversario) }}</span></div>
              <div><strong>Estado</strong><span>{{ estadoCasamiento(c) }}</span></div>
            </div>
            <div class="seccion">Objeciones tratadas</div>
            @if (c.objecionesTratadas.length) {
              <ul class="objeciones">
                @for (o of c.objecionesTratadas; track $index) {
                  <li><strong>{{ categoria(o.categoria) }}</strong> ({{ o.fundada ? 'fundada' : 'no fundada' }}): {{ o.respuesta ?? 'sin respuesta todavía' }}</li>
                }
              </ul>
            } @else {
              <p class="nadie">Nadie se opuso. Que hablen ahora o callen para siempre.</p>
            }
            @if (c.informeAniversario) {
              <div class="seccion">Informe de aniversario · uso real</div>
              <div class="express">{{ c.informeAniversario }}</div>
            }
            <p class="lema">«Hasta que la burocracia nos separe»</p>
            <div class="firmas"><div>Juez de paz</div><div>Dueño</div><div>Padrino</div></div>
          </div>
        }

        @if (esNecro() && velorio(); as v) {
          <div class="necro" data-print="1">
            <div class="cruz">✝</div>
            <div class="titulo-acta">ACTA DE DEFUNCIÓN N.º {{ v.numeroActa }}</div>
            <p class="intro">Con profundo alivio despedimos a</p>
            <h1>{{ v.codigo }} {{ v.nombre }}</h1>
            <p class="fechas">✶ {{ fCorta(v.fechaNacimiento) }} &nbsp;—&nbsp; ✝ {{ fCorta(v.fecha) }} · versión {{ v.version }}</p>
            <p class="epitafio">{{ v.epitafio }}</p>
            <div class="datos-necro">
              <div><div class="k">Causa</div><div class="v">{{ v.causa }}</div></div>
              <div><div class="k">Tiempo que nos robaba</div><div class="v">{{ v.minutosSemanalesPerdidos }} minutos por semana</div></div>
              <div><div class="k">Herencia</div><div class="v">{{ v.herencia ?? 'Nada que rescatar.' }}</div></div>
              <div><div class="k">Lecciones</div><div class="v">{{ v.lecciones ?? '—' }}</div></div>
              <div>
                <div class="k">Reencarnación</div>
                <div class="v">
                  @if (v.reencarnacionCodigo) {
                    {{ v.reencarnacionCodigo }} «{{ v.reencarnacionNombre }}»
                    @if (v.reencarnacionEstado === 'BORRADOR') { <span class="borrador">en borrador</span> }
                  } @else {
                    Sin reemplazo: {{ v.sinReemplazoMotivo }}
                  }
                </div>
              </div>
              <div><div class="k">Deudos</div><div class="v">{{ v.deudos }}</div></div>
            </div>
            <p class="norma">
              Se conserva como documento obsoleto según ISO 9001 7.5.3 (control de la información documentada). Q.E.P.D.
            </p>
            <div class="pie">
              <div class="firmas-acta">
                @for (f of v.firmas; track f.rol) {
                  <div class="firma" [class.ok]="f.usuarioId">
                    <span class="ms">{{ f.usuarioId ? 'verified' : 'pending' }}</span>
                    <div>
                      <div class="k">{{ f.etiqueta }}{{ f.rol === 'DUENO' && v.duenoNombre ? ' · ' + v.duenoNombre : '' }}</div>
                      <div class="v">{{ f.usuarioId ? f.nombre + ' · ' + fFirma(f.fecha!) : 'Pendiente' }}</div>
                    </div>
                  </div>
                }
              </div>
              @if (v.completa) {
                <p class="firmada">✔ Acta completa: {{ v.codigo }} quedó velado y descansa en el archivo de obsoletos.</p>
              } @else if (puedoFirmar(v)) {
                <button class="firmar" (pointerdown)="apretar()" (pointerup)="soltar()" (pointerleave)="soltar()"
                        (pointercancel)="soltar()" (contextmenu)="$event.preventDefault()" [disabled]="firmando()">
                  <span class="relleno" [style.width.%]="progreso() * 100"></span>
                  <span class="etiqueta" [style.color]="progreso() > .5 ? '#3D2600' : '#F2A541'">
                    <span class="ms">draw</span>{{ firmando() ? 'Firmando…' : 'Firmar (mantené apretado)' }}
                  </span>
                </button>
                <p class="aviso">Con las tres firmas el procedimiento queda velado para siempre.</p>
              } @else {
                <p class="pendiente">Faltan firmas. Tu firma no corresponde o ya está.</p>
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
    .postergado { display: flex; gap: 8px; align-items: center; margin: 0 8px 10px; padding: 10px 14px; border-radius: 12px;
      background: #FBE9CC; color: #6B4300; font-size: 13px; font-weight: 600; font-family: var(--sans); }
    .postergado .ms { font-size: 20px; }
    .pcts { display: grid; grid-template-columns: repeat(auto-fit, minmax(90px, 1fr)); gap: 6px; }
    .pct { display: flex; flex-direction: column; padding: 8px; background: #fff; border: 1px dashed #C9B28A; }
    .pct .n { font-size: 22px; color: #2E7D73; }
    .pct .a { font-family: var(--sans); font-size: 10.5px; color: #7A5E2E; }
    .objeciones { margin: 0; padding-left: 18px; text-align: left; font-family: var(--sans); font-size: 13px; line-height: 1.5; }
    .nadie { margin: 0; font-style: italic; font-size: 14px; color: #7A5E2E; }
    .titulo-acta { margin-top: 10px; font-family: var(--sans); font-size: 12px; font-weight: 700; letter-spacing: .18em; color: var(--ambar); }
    .borrador { display: inline-block; margin-left: 4px; padding: 1px 7px; border-radius: 999px; background: #3B3F40; font-size: 11px; color: #F2F2F2; }
    .firmas-acta { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; text-align: left; }
    .firma { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 12px; border: 1px dashed #4B5153; }
    .firma .ms { font-size: 22px; color: #9AA3A5; }
    .firma.ok { border-style: solid; border-color: #2E7D73; }
    .firma.ok .ms { color: #9FD3CB; }

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
  readonly usuario = this.auth.usuario;
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

  /** El botón se ve si me toca una firma pendiente: juez de paz, Dirección o dueño. Una persona, una firma. */
  puedoFirmar(v: Velorio): boolean {
    const u = this.usuario();
    if (!u || v.firmas.some((f) => f.usuarioId === u.id)) return false;
    return v.firmas.some(
      (f) =>
        !f.usuarioId &&
        ((f.rol === 'JUEZ_PAZ' && u.rol === 'JUEZ_PAZ') ||
          (f.rol === 'DIRECCION' && u.rol === 'DIRECCION') ||
          (f.rol === 'DUENO' && v.duenoId === u.id)),
    );
  }

  testigos(c: Casamiento): string {
    return c.testigos.length ? lista(c.testigos.map((t) => `${t.nombre} (${t.areaNombre})`)) : 'Sin testigos';
  }

  categoria(c: CategoriaObjecion): string {
    return CATEGORIAS[c];
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
        this.ui.ok(firmado.completa ? `${v.codigo} descansa en paz: acta completa.` : 'Firma registrada. Faltan las otras.');
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
