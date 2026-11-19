import { Component, HostListener, OnInit, TemplateRef, computed, effect, inject, signal, untracked, viewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, forkJoin, map, of, tap, timer } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { CATEGORIAS, CATEGORIAS_LISTA, fDiaMes, iconoTipo } from '../../core/diseno';
import { CategoriaObjecion, ObjecionNueva, Perfil, ValorVoto } from '../../core/models';
import { UiService } from '../../core/ui.service';

/** Distancia (px) a partir de la cual un arrastre cuenta como voto. */
const UMBRAL = 110;
const MS_SALIDA = 360;
const EASE = 'transform .4s cubic-bezier(.2,.8,.25,1)';

@Component({
  selector: 'app-votar',
  imports: [FormsModule],
  template: `
    <div class="cab">
      <h1 class="titulo-pagina">¿Te sirve o te sobra?</h1>
      <p class="matriz"><span class="ms">checklist</span>Votás solo los procedimientos en los que participás</p>
      <div class="progreso">
        <div class="pista"><div class="lleno" [style.width.%]="pct()"></div></div>
        <span>{{ etiqueta() }}</span>
      </div>
    </div>

    @if (cargando()) {
      <div class="mazo"><div class="esqueleto"></div></div>
    } @else if (!ui.ciclo() && !actual() && !votados()) {
      <div class="vacio-punteado">
        <span class="ms" style="font-size: 40px; color: var(--gris-claro)">bedtime</span>
        <h2>No hay ciclo de votación abierto</h2>
        <p>El juez de paz todavía no abrió el trimestre. Mientras tanto, mirá los resultados o andá a un casamiento.</p>
      </div>
    } @else if (!actual()) {
      <div class="vacio-punteado">
        <div class="anillos" style="--d: 38px; --b: 3px"></div>
        <h2>No hay más procedimientos para vos este trimestre.</h2>
        <p>{{ textoFin() }}</p>
        <button class="ver" (click)="irAResultados()">Ver resultados de tu área</button>
      </div>
    } @else {
      <div class="mazo">
        @for (c of pila(); track c.p.id) {
          <div class="pos" [class.arriba]="c.arriba" [class.agarrando]="c.arriba && arrastrando()"
               [style.transform]="c.arriba ? tArriba() : tAtras()" [style.transition]="transicion()"
               (pointerdown)="apretar($event, c.arriba)" (pointermove)="mover($event)"
               (pointerup)="soltar()" (pointercancel)="soltar()">
            <div class="carta" [style.boxShadow]="c.arriba ? (arrastrando() ? '0 22px 44px rgba(18,52,59,.2)' : '0 12px 30px rgba(18,52,59,.13)') : '0 4px 14px rgba(18,52,59,.08)'">
              <div class="tapa">
                <div class="fila">
                  <span class="tipo"><span class="ms">{{ iconoTipo(c.p.tipo) }}</span>{{ c.p.tipo === 'REGISTRO' ? 'Registro' : 'Procedimiento' }}</span>
                  @if (c.p.votacionRapida) {
                    <span class="rapida" title="Votación de 7 días después de una segunda cita"><span class="ms">bolt</span>Votación rápida</span>
                  }
                  <div class="flex"></div>
                  @if (c.p.obligatorio) {
                    <span class="oblig" [title]="c.p.baseObligatoriedad ?? ''"><span class="ms">gavel</span>Obligatorio por norma</span>
                  }
                </div>
                <div class="pie-tapa">
                  <span class="cod">{{ c.p.codigo }}@if (c.p.version > 1) {<small> v{{ c.p.version }}</small>}</span>
                  <span class="areas">{{ c.p.areas.join(' · ') }}</span>
                </div>
              </div>
              <div class="cuerpo">
                <h2>{{ c.p.nombre }}, <span class="edad">{{ c.p.edadAnios }} {{ c.p.edadAnios === 1 ? 'año' : 'años' }}</span></h2>
                <p class="bio">“{{ c.p.bio }}”</p>
                <div class="datos">
                  <span class="k"><span class="ms">schedule</span>Te llevo</span><span class="v">{{ c.p.minutosPorUso }} min</span>
                  <span class="k"><span class="ms">redeem</span>Te doy</span><span class="v">{{ c.p.beneficio }}</span>
                  <span class="k"><span class="ms">group</span>Me usan</span><span class="v">{{ uso(c.p) }}</span>
                  <span class="k"><span class="ms">person</span>Dueño</span>
                  <span class="v" [class.huerfano]="!c.p.duenoNombre">{{ c.p.duenoNombre ?? 'sin dueño (¡ojo!)' }}</span>
                  <span class="k"><span class="ms">volunteer_activism</span>Padrino</span>
                  <span class="v" [class.huerfano]="!c.p.padrinoNombre">{{ c.p.padrinoNombre ?? 'nadie se hace cargo' }}</span>
                </div>
              </div>
              <div class="lavado" [style.background]="sirveOp() >= sobraOp() ? '#2E7D73' : '#D0573F'" [style.opacity]="c.arriba ? prog() * .06 : 0"></div>
              <div class="sello sirve" [style.opacity]="c.arriba ? sirveOp() : 0">ME SIRVE</div>
              <div class="sello sobra" [style.opacity]="c.arriba ? sobraOp() : 0">ME SOBRA</div>
            </div>
          </div>
        }
      </div>
      <div class="botones">
        <button class="nope" (click)="votar('NOPE')" aria-label="Me sobra" [style.transform]="'scale(' + (1 + .14 * sobraOp()) + ')'">
          <span class="ms">close</span>
        </button>
        <button class="like" (click)="votar('LIKE')" aria-label="Me sirve" [style.transform]="'scale(' + (1 + .14 * sirveOp()) + ')'">
          <span class="ms relleno">favorite</span>
        </button>
      </div>
    }

    <div class="anonimo"><span class="ms">lock</span>Tu voto es anónimo: solo se ven resultados por área</div>

    <ng-template #hojaObjecion>
      <div class="titulo-hoja">¿Por qué te sobra?</div>
      <p class="sub-hoja">Opcional y anónimo: no queda tu nombre en ningún lado. El juez de paz la lee y nadie queda sin respuesta.</p>
      <div class="form">
        <label class="campo">
          <span>Categoría</span>
          <select [(ngModel)]="obj.categoria" name="categoria">
            <option [ngValue]="null" disabled>Elegí una…</option>
            @for (c of categorias; track c) {
              <option [ngValue]="c">{{ etiquetas[c] }}</option>
            }
          </select>
        </label>
        <label class="campo">
          <span>Contanos un poco más (opcional)</span>
          <textarea rows="3" maxlength="1000" [(ngModel)]="obj.texto" name="texto" placeholder="Ej.: son 5 preguntas y nadie las contesta"></textarea>
        </label>
        @if (errorObj()) {
          <div class="error-form">{{ errorObj() }}</div>
        }
        <button class="btn btn-petroleo" (click)="enviarObjecion()">Enviar objeción</button>
        <button class="btn-texto" (click)="saltar()">Saltar</button>
      </div>
    </ng-template>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .cab { padding: 6px 22px 14px; }
    .matriz { display: flex; align-items: center; gap: 6px; margin: 4px 0 0; font-size: 12.5px; color: var(--gris); }
    .matriz .ms { font-size: 16px; color: var(--match); }
    .rapida { display: inline-flex; align-items: center; gap: 3px; padding: 4px 10px 4px 7px; border-radius: 999px; font-size: 11.5px;
      font-weight: 700; background: #6FD0C2; color: var(--petroleo); }
    .rapida .ms { font-size: 15px; }
    .cod small { font-size: 18px; letter-spacing: 0; opacity: .7; }
    .sub-hoja { margin: 6px 0 0; font-size: 13px; line-height: 1.45; color: var(--gris); }
    .progreso { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
    .pista { flex: 1; height: 4px; border-radius: 4px; background: rgba(18,52,59,.1); overflow: hidden; }
    .lleno { height: 100%; background: var(--ambar); border-radius: 4px; transition: width .4s; }
    .progreso span { font-size: 12.5px; color: var(--gris); font-variant-numeric: tabular-nums; }

    .mazo { flex: 1; position: relative; margin: 0 18px; min-height: 0; }
    .esqueleto { position: absolute; inset: 0; border-radius: 28px; background: rgba(18,52,59,.06); animation: latir 1.2s ease-in-out infinite; }
    @keyframes latir { 50% { opacity: .5; } }
    .pos { position: absolute; inset: 0; touch-action: pan-y; user-select: none; -webkit-user-select: none; z-index: 1; }
    .pos.arriba { z-index: 2; cursor: grab; }
    .pos.agarrando { cursor: grabbing; }
    .carta { position: relative; height: 100%; border-radius: 28px; background: var(--papel); overflow: hidden; display: flex; flex-direction: column; container: carta / size; }
    .tapa {
      flex: none; height: 38%; min-height: 150px; padding: 16px 18px 18px; display: flex; flex-direction: column; color: var(--crema);
      background-color: var(--petroleo); background-image: repeating-linear-gradient(135deg, rgba(255,255,255,.035) 0 1px, transparent 1px 9px);
    }
    .fila { display: flex; align-items: center; gap: 6px; }
    .tipo, .oblig { display: inline-flex; align-items: center; gap: 5px; padding: 4px 10px 4px 8px; border-radius: 999px; font-size: 11.5px; }
    .tipo { border: 1px solid rgba(245,240,230,.28); letter-spacing: .03em; }
    .oblig { background: var(--ambar); color: var(--ambar-tinta); font-weight: 600; }
    .tipo .ms, .oblig .ms { font-size: 15px; }
    .pie-tapa { flex: 1; display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; }
    .cod { font-family: var(--serif); font-size: 62px; line-height: .85; letter-spacing: -.03em; white-space: nowrap; }
    .areas { font-size: 11.5px; line-height: 1.35; text-align: right; color: rgba(245,240,230,.72); max-width: 46%; }
    .cuerpo { flex: 1; min-height: 0; padding: 18px 20px 14px; display: flex; flex-direction: column; gap: 10px; overflow-x: hidden; overflow-y: auto; overscroll-behavior: contain; container: cuerpo / size; }
    .cuerpo h2 { margin: 0; font-family: var(--serif); font-weight: 500; font-size: 25px; line-height: 1.08; letter-spacing: -.015em; color: var(--petroleo); text-wrap: balance; }
    .edad { color: #7E8C8F; font-weight: 400; }
    .bio {
      margin: 0; font-family: var(--serif); font-style: italic; font-size: 16.5px; line-height: 1.38; color: var(--texto-2); text-wrap: pretty;
      flex: none; display: -webkit-box; -webkit-line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden;
    }
    /* La bio se recorta en líneas enteras según el alto que le queda a la carta */
    /* (umbrales pensados para 5 filas de datos: te llevo, te doy, me usan, dueño y padrino) */
    @container cuerpo (max-height: 329px) { .bio { -webkit-line-clamp: 3; } }
    @container cuerpo (max-height: 306px) { .bio { -webkit-line-clamp: 2; } }
    @container cuerpo (max-height: 283px) { .bio { -webkit-line-clamp: 1; } }
    @container cuerpo (max-height: 260px) { .bio { display: none; } }
    /* Pantallas bajas (ventana chica o celular con barra): tapa más baja y datos más apretados */
    @container carta (max-height: 420px) {
      .tapa { height: auto; min-height: 0; padding: 12px 16px 14px; }
      .pie-tapa { margin-top: 14px; }
      .cod { font-size: 46px; }
      .cuerpo { padding: 12px 18px 10px; gap: 6px; }
      .cuerpo h2 { font-size: 21px; }
    }
    @container carta (max-height: 380px) {
      .tapa { padding: 10px 14px 10px; }
      .pie-tapa { margin-top: 8px; }
      .cod { font-size: 36px; }
      .cuerpo h2 { font-size: 19px; }
      .cuerpo .datos { font-size: 12.5px; }
      .cuerpo .datos > span { padding-top: 4px; padding-bottom: 4px; }
    }
    .datos { margin-top: auto; display: grid; grid-template-columns: auto minmax(0, 1fr); font-size: 13.5px; }
    .datos > span { padding: 7px 0; border-top: 1px solid rgba(18,52,59,.08); }
    .k { display: flex; align-items: center; gap: 6px; padding-right: 14px !important; color: var(--gris); font-size: 12px; }
    .k .ms { font-size: 16px; }
    .v { color: var(--texto); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .v.huerfano { color: var(--gris); font-style: italic; }
    .lavado { position: absolute; inset: 0; pointer-events: none; }
    .sello {
      position: absolute; top: 30px; padding: 6px 14px; border-radius: 10px; background: rgba(255,252,246,.95);
      font-family: var(--serif); font-weight: 600; font-size: 28px; letter-spacing: .08em; pointer-events: none;
    }
    .sirve { left: 18px; transform: rotate(-12deg); border: 3px solid #2E7D73; box-shadow: inset 0 0 0 2px #FFFCF6, inset 0 0 0 3.5px #2E7D73; color: #2E7D73; }
    .sobra { right: 18px; transform: rotate(12deg); border: 3px solid #D0573F; box-shadow: inset 0 0 0 2px #FFFCF6, inset 0 0 0 3.5px #D0573F; color: #C24E37; }

    .botones { flex: none; display: flex; justify-content: center; align-items: center; gap: 30px; padding: 16px 0 4px; }
    .botones button { width: 64px; height: 64px; border-radius: 50%; border: none; display: grid; place-items: center; cursor: pointer; transition: transform .15s; }
    .botones .ms { font-size: 30px; }
    .nope { background: var(--papel); color: var(--rechazo); box-shadow: 0 6px 18px rgba(18,52,59,.12), 0 0 0 1px rgba(208,87,63,.22); }
    .nope .ms { font-variation-settings: 'wght' 500; }
    .like { background: var(--match); color: var(--papel); box-shadow: 0 8px 20px rgba(46,125,115,.35); }

    .ver { margin-top: 6px; height: 46px; padding: 0 20px; border-radius: 999px; border: 1px solid var(--petroleo); background: transparent; color: var(--petroleo); font-weight: 600; cursor: pointer; }
    .anonimo { flex: none; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 12px 24px 14px; font-size: 12px; color: var(--gris); }
    .anonimo .ms { font-size: 15px; }
  `,
})
export class VotarComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly ui = inject(UiService);
  readonly iconoTipo = iconoTipo;

  readonly cargando = signal(true);
  readonly mazo = signal<Perfil[]>([]);
  readonly votados = signal(0);
  readonly dx = signal(0);
  readonly dy = signal(0);
  readonly arrastrando = signal(false);
  /** 1 = sale a la derecha (me sirve), -1 = a la izquierda (me sobra). */
  readonly salida = signal<0 | 1 | -1>(0);
  readonly sinAnimacion = signal(false);
  /** Carta que salió como NOPE y espera "Enviar objeción" o "Saltar". */
  private readonly pendiente = signal<Perfil | null>(null);
  readonly errorObj = signal<string | null>(null);
  private readonly hojaObjecion = viewChild.required<TemplateRef<unknown>>('hojaObjecion');
  readonly categorias = CATEGORIAS_LISTA;
  readonly etiquetas = CATEGORIAS;
  obj: { categoria: CategoriaObjecion | null; texto: string } = { categoria: null, texto: '' };
  private sx = 0;
  private sy = 0;

  readonly actual = computed(() => this.mazo()[0] ?? null);
  /** La carta de arriba va primero en el DOM: al votar, la de atrás no se mueve de lugar. */
  readonly pila = computed(() => {
    const [a, b] = this.mazo();
    const pila: { p: Perfil; arriba: boolean }[] = [];
    if (a) pila.push({ p: a, arriba: true });
    if (b) pila.push({ p: b, arriba: false });
    return pila;
  });
  readonly total = computed(() => this.votados() + this.mazo().length);
  readonly pct = computed(() => (this.total() ? Math.round((this.votados() / this.total()) * 100) : 0));
  readonly etiqueta = computed(() => {
    if (this.cargando() || (!this.ui.ciclo() && !this.total())) return '—';
    return this.actual() ? `${this.votados() + 1} de ${this.total()}` : 'Listo';
  });

  readonly prog = computed(() => (this.salida() ? 1 : Math.min(1, Math.abs(this.dx()) / UMBRAL)));
  readonly sirveOp = computed(() => (this.salida() === 1 ? 1 : this.salida() === -1 ? 0 : clamp01(this.dx() / UMBRAL)));
  readonly sobraOp = computed(() => (this.salida() === -1 ? 1 : this.salida() === 1 ? 0 : clamp01(-this.dx() / UMBRAL)));
  readonly tArriba = computed(() => {
    const s = this.salida();
    return s
      ? `translate(${s * 160}%, ${this.dy() * 0.4}px) rotate(${s * 24}deg)`
      : `translate(${this.dx()}px, ${this.dy() * 0.25}px) rotate(${this.dx() / 16}deg)`;
  });
  readonly tAtras = computed(() => `translateY(${14 * (1 - this.prog())}px) scale(${0.94 + 0.06 * this.prog()})`);
  readonly transicion = computed(() => (this.arrastrando() || this.sinAnimacion() ? 'none' : EASE));

  readonly textoFin = computed(() => {
    const n = this.votados();
    const c = this.ui.ciclo();
    const votaste = n ? `Votaste ${n} ${n === 1 ? 'procedimiento' : 'procedimientos'}. ` : 'Ya votaste todo el mazo. ';
    return c ? `${votaste}El ciclo ${c.nombre} cierra el ${fDiaMes(c.fechaFin)}: ahí se sabe quién se casa y quién se va.` : votaste;
  });

  constructor() {
    // Si la hoja de objeción se cierra sin elegir (velo o Escape), la carta vuelve al mazo.
    effect(() => {
      const abierta = this.ui.hojaAbierta();
      untracked(() => {
        if (!abierta && this.pendiente()) {
          this.pendiente.set(null);
          this.salida.set(0);
          this.dx.set(0);
          this.dy.set(0);
        }
      });
    });
  }

  ngOnInit(): void {
    this.api.mazo().subscribe({
      next: (m) => {
        this.mazo.set(m);
        this.cargando.set(false);
      },
      error: (e) => {
        this.ui.error(e);
        this.cargando.set(false);
      },
    });
  }

  uso(p: Perfil): string {
    return p.usoTexto.replace(/^Me usan\s+/i, '');
  }

  irAResultados(): void {
    this.router.navigate(['/resultados']);
  }

  @HostListener('window:keydown', ['$event'])
  teclado(e: KeyboardEvent): void {
    if (this.ui.hojaAbierta() || this.ui.docAbierto()) return;
    if (e.key === 'ArrowRight') {
      e.preventDefault();
      this.votar('LIKE');
    } else if (e.key === 'ArrowLeft') {
      e.preventDefault();
      this.votar('NOPE');
    }
  }

  apretar(e: PointerEvent, arriba: boolean): void {
    if (!arriba || this.salida()) return;
    (e.currentTarget as HTMLElement).setPointerCapture?.(e.pointerId);
    this.sx = e.clientX;
    this.sy = e.clientY;
    this.dx.set(0);
    this.dy.set(0);
    this.arrastrando.set(true);
  }

  mover(e: PointerEvent): void {
    if (!this.arrastrando()) return;
    this.dx.set(e.clientX - this.sx);
    this.dy.set(e.clientY - this.sy);
  }

  soltar(): void {
    if (!this.arrastrando()) return;
    const dx = this.dx();
    if (dx > UMBRAL) this.votar('LIKE');
    else if (dx < -UMBRAL) this.votar('NOPE');
    else {
      this.arrastrando.set(false);
      this.dx.set(0);
      this.dy.set(0);
    }
  }

  /** La carta sale volando enseguida. Con NOPE se ofrece objetar (anónimo); el voto viaja después. */
  votar(valor: ValorVoto): void {
    const p = this.actual();
    if (!p || this.salida()) return;
    this.arrastrando.set(false);
    this.salida.set(valor === 'LIKE' ? 1 : -1);
    if (valor === 'NOPE') {
      this.obj = { categoria: null, texto: '' };
      this.errorObj.set(null);
      this.pendiente.set(p);
      this.ui.abrirHoja(this.hojaObjecion());
      return;
    }
    this.enviar(p, 'LIKE', null);
  }

  enviarObjecion(): void {
    const p = this.pendiente();
    if (!p) return;
    if (!this.obj.categoria) {
      this.errorObj.set('Elegí una categoría (o tocá "Saltar" para votar sin objetar).');
      return;
    }
    this.enviar(p, 'NOPE', { categoria: this.obj.categoria, texto: this.obj.texto.trim() || null });
  }

  saltar(): void {
    const p = this.pendiente();
    if (p) this.enviar(p, 'NOPE', null);
  }

  private enviar(p: Perfil, valor: ValorVoto, objecion: ObjecionNueva | null): void {
    this.pendiente.set(null);
    this.ui.cerrarHoja();
    const voto$ = this.api.votar(p.id, p.cicloId, valor, objecion).pipe(
      tap((r) => {
        if (r.match) this.ui.mostrarMatch(`${p.codigo} ${p.nombre}`);
        else if (objecion) this.ui.ok('Objeción anónima enviada. Te van a responder en el tablero.');
      }),
      map(() => null as unknown),
      catchError((e: unknown) => of(e)),
    );
    forkJoin([voto$, timer(MS_SALIDA)]).subscribe(([error]) => {
      if (error) {
        this.ui.error(error);
        const yaNoSeVota = error instanceof HttpErrorResponse && [403, 409, 422].includes(error.status);
        if (!yaNoSeVota) {
          this.salida.set(0);
          this.dx.set(0);
          this.dy.set(0);
          return;
        }
      }
      this.sinAnimacion.set(true);
      this.mazo.update((m) => m.slice(1));
      this.votados.update((n) => n + 1);
      this.salida.set(0);
      this.dx.set(0);
      this.dy.set(0);
      setTimeout(() => this.sinAnimacion.set(false), 40);
    });
  }
}

function clamp01(n: number): number {
  return Math.max(0, Math.min(1, n));
}
