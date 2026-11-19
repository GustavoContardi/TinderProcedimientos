import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { CATEGORIAS, DECISIONES, fCorta, fDiaMes } from '../../core/diseno';
import { DecisionSegundaCita, GrupoObjeciones, SegundaCita } from '../../core/models';
import { UiService, mensajeDeError } from '../../core/ui.service';

const DURACION = 30 * 60;
/** [desde, hasta (minutos), nombre] */
const BLOQUES: readonly (readonly [number, number, string])[] = [
  [0, 5, 'Presentación'],
  [5, 15, 'Debate'],
  [15, 25, 'Propuestas'],
  [25, 30, 'Decisión'],
];

/** Segundas citas: reunión de 30 minutos con cronómetro, objeciones agrupadas, minuta y decisión. */
@Component({
  selector: 'app-segundas-citas',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="pantalla">
      @if (!sel()) {
        <a class="volver" routerLink="/juez-de-paz"><span class="ms">arrow_back</span>Juez de paz</a>
        <h1 class="titulo-pagina">Segundas citas</h1>
        <p class="subtitulo">30 minutos para escuchar a la minoría y decidir. Ninguna decisión cambia el destino sin votación.</p>

        <div class="rotulo">Convocadas</div>
        <div class="lista" style="margin-bottom: 22px">
          @for (c of convocadas(); track c.id) {
            <button class="tarjeta cita" (click)="abrir(c)">
              <div class="fecha"><span class="d">{{ dia(c.fechaProgramada) }}</span><span class="m">{{ mes(c.fechaProgramada) }}</span></div>
              <div class="flex" style="min-width: 0">
                <div class="codigo">{{ c.procedimiento.codigo }} · {{ c.cicloNombre ?? 'boda / aniversario' }}</div>
                <div class="nombre-serif">{{ c.procedimiento.nombre }}</div>
                <div class="meta">{{ c.motivo }}</div>
                @if (c.procedimiento.requiereReformulacion) {
                  <span class="chip" style="margin-top: 6px; background: #F8E1DB; color: #8A2F1D">{{ c.procedimiento.segundasCitasConsecutivas }}.ª cita seguida: requiere reformulación</span>
                }
              </div>
              <span class="ms chevron">chevron_right</span>
            </button>
          } @empty {
            <div class="vacio-italico">No hay citas pendientes. Todos saben lo que quieren.</div>
          }
        </div>

        @if (realizadas().length) {
          <div class="rotulo">Ya realizadas</div>
          <div class="lista">
            @for (c of realizadas(); track c.id) {
              <div class="tarjeta realizada">
                <div class="codigo">{{ c.procedimiento.codigo }} · {{ fCorta(c.fechaProgramada) }}</div>
                <div class="nombre-serif" style="font-size: 16px">{{ decisiones[c.decision!][0] }}</div>
                @if (c.cicloRapido; as r) {
                  <div class="meta"><span class="ms" style="font-size: 14px">bolt</span><span>{{ r.nombre }} · {{ r.estado === 'ABIERTO' ? 'votando hasta el ' + fDiaMes(r.fechaFin) : 'cerrada' }}</span></div>
                } @else {
                  <div class="meta">Espera al próximo Tinder.</div>
                }
              </div>
            }
          </div>
        }
      } @else {
        @let c = sel()!;
        <button class="volver boton" (click)="cerrar()"><span class="ms">arrow_back</span>Segundas citas</button>
        <div class="codigo">{{ c.procedimiento.codigo }} · v{{ c.procedimiento.version }} · dueño: {{ c.procedimiento.duenoNombre ?? 'sin dueño' }}</div>
        <h1 class="titulo-pagina" style="font-size: 26px">{{ c.procedimiento.nombre }}</h1>
        @if (c.pctGlobal !== null) {
          <p class="subtitulo">{{ c.pctGlobal }} % me sirve · votaron {{ c.votantes }} de {{ c.habilitados }} · {{ c.motivo }}</p>
        } @else {
          <p class="subtitulo">{{ c.motivo }}</p>
        }

        <div class="crono">
          <div class="reloj">
            <span class="tiempo">{{ reloj() }}</span>
            <span class="bloque-actual">{{ bloqueActual() }}</span>
          </div>
          <div class="bloques">
            @for (b of bloques; track b[2]; let i = $index) {
              <div class="bloque" [style.flex]="b[1] - b[0]" [class.activo]="indiceBloque() === i" [class.pasado]="indiceBloque() > i">
                <div class="relleno" [style.width.%]="avanceBloque(i)"></div>
                <span>{{ b[2] }}</span><small>{{ b[0] }}–{{ b[1] }}′</small>
              </div>
            }
          </div>
          <div class="crono-botones">
            <button class="btn btn-ambar" (click)="alternarReloj()">
              <span class="ms">{{ corriendo() ? 'pause' : 'play_arrow' }}</span>{{ corriendo() ? 'Pausar' : segundos() ? 'Seguir' : 'Empezar la cita' }}
            </button>
            <button class="btn-texto" (click)="reiniciar()">Reiniciar</button>
          </div>
        </div>

        <div class="rotulo" style="margin-top: 18px">Lo que dijo la minoría</div>
        @for (g of objeciones(); track g.procedimientoId) {
          @for (cat of g.categorias; track cat.categoria) {
            <div class="tarjeta obj">
              <div class="obj-cab"><span class="cant">{{ cat.cantidad }}</span>{{ categorias[cat.categoria] }}</div>
              @for (o of cat.objeciones; track o.id) {
                @if (o.texto) { <div class="obj-texto">«{{ o.texto }}»</div> }
              }
            </div>
          }
        } @empty {
          <div class="meta" style="margin-bottom: 8px">Nadie objetó por escrito: solo hubo votos divididos.</div>
        }

        <div class="rotulo" style="margin-top: 18px">Minuta y decisión</div>
        <div class="form" style="margin-top: 0">
          <label class="campo"><span>Minuta</span><textarea rows="4" [(ngModel)]="f.minuta" placeholder="Quiénes estuvieron, qué se dijo, qué se propuso"></textarea></label>
          <div class="campo">
            <span>Decisión</span>
            @for (d of ordenDecisiones; track d) {
              <button class="decision" [class.on]="f.decision === d" (click)="f.decision = d"
                      [disabled]="d === 'VELORIO' && c.procedimiento.obligatorio">
                <strong>{{ decisiones[d][0] }}</strong>
                <span>{{ d === 'VELORIO' && c.procedimiento.obligatorio ? 'No disponible: es obligatorio por norma (se simplifica, no se vela).' : decisiones[d][1] }}</span>
              </button>
            }
          </div>
          @if (f.decision === 'CASAR_CON_CONDICIONES') {
            <label class="campo"><span>Condiciones (van a la libreta)</span><textarea rows="2" [(ngModel)]="f.condiciones" placeholder="Ej.: una sola pregunta con caritas"></textarea></label>
          }
          @if (error()) { <div class="error-form">{{ error() }}</div> }
          @if (f.decision === 'AJUSTAR_Y_REVOTAR') {
            <button class="btn btn-petroleo" (click)="realizar(false)" [disabled]="ocupado()">Esperar al próximo Tinder</button>
          }
          <button class="btn btn-ambar" (click)="realizar(true)" [disabled]="ocupado() || !f.decision">
            <span class="ms">bolt</span>{{ ocupado() ? 'Lanzando…' : 'Lanzar votación rápida (7 días)' }}
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .volver { display: inline-flex; align-items: center; gap: 4px; margin-bottom: 12px; font-size: 13px; font-weight: 600; color: var(--petroleo); text-decoration: none; }
    .volver.boton { border: none; background: none; padding: 0; cursor: pointer; }
    .volver .ms { font-size: 18px; }
    .cita { border: none; text-align: left; cursor: pointer; padding: 14px; display: flex; gap: 12px; align-items: center; width: 100%; }
    .fecha { flex: none; width: 52px; padding: 6px 0; border-radius: 12px; background: #FBE9CC; color: #6B4300; display: flex; flex-direction: column; align-items: center; }
    .fecha .d { font-family: var(--serif); font-size: 24px; line-height: 1; }
    .fecha .m { font-size: 10.5px; font-weight: 700; text-transform: uppercase; }
    .meta { font-size: 12.5px; line-height: 1.4; color: var(--gris); margin-top: 3px; display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
    .chevron { font-size: 20px; color: var(--gris-claro); }
    .realizada { padding: 12px 14px; }
    .crono { padding: 16px; border-radius: 20px; color: var(--crema); display: flex; flex-direction: column; gap: 14px;
      background-color: var(--petroleo); background-image: repeating-linear-gradient(135deg, rgba(255,255,255,.035) 0 1px, transparent 1px 9px); }
    .reloj { display: flex; align-items: baseline; justify-content: space-between; }
    .tiempo { font-family: var(--serif); font-size: 52px; line-height: 1; font-variant-numeric: tabular-nums; }
    .bloque-actual { font-size: 13px; font-weight: 600; letter-spacing: .08em; text-transform: uppercase; color: var(--ambar); }
    .bloques { display: flex; gap: 4px; }
    .bloque { position: relative; overflow: hidden; min-width: 0; padding: 6px 6px 5px; border-radius: 8px; background: rgba(245,240,230,.1);
      display: flex; flex-direction: column; font-size: 10.5px; line-height: 1.2; }
    .bloque span, .bloque small { position: relative; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .bloque small { opacity: .7; }
    .bloque .relleno { position: absolute; inset: 0 auto 0 0; background: rgba(242,165,65,.35); }
    .bloque.activo { outline: 2px solid var(--ambar); }
    .bloque.pasado { opacity: .6; }
    .crono-botones { display: flex; align-items: center; gap: 8px; }
    .crono-botones .btn { flex: 1; height: 44px; }
    .crono-botones .btn-texto { color: rgba(245,240,230,.75); }
    .obj { padding: 12px 14px; margin-bottom: 8px; display: flex; flex-direction: column; gap: 6px; }
    .obj-cab { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 13.5px; color: var(--petroleo); }
    .cant { min-width: 24px; height: 24px; padding: 0 6px; border-radius: 12px; background: var(--ambar); color: var(--ambar-tinta);
      display: grid; place-items: center; font-size: 12px; font-weight: 700; }
    .obj-texto { font-family: var(--serif); font-style: italic; font-size: 14.5px; color: var(--texto-2); }
    .decision { display: flex; flex-direction: column; gap: 3px; text-align: left; padding: 12px 14px; border-radius: 12px; cursor: pointer;
      border: 1px solid rgba(18,52,59,.14); background: #fff; color: var(--texto-2); font-size: 12.5px; line-height: 1.4; }
    .decision strong { font-size: 14px; color: var(--petroleo); }
    .decision.on { border-color: var(--petroleo); box-shadow: var(--foco); }
    .decision:disabled { opacity: .55; cursor: default; }
  `,
})
export class SegundasCitasComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly ui = inject(UiService);
  readonly fCorta = fCorta;
  readonly fDiaMes = fDiaMes;
  readonly bloques = BLOQUES;
  readonly decisiones = DECISIONES;
  readonly categorias = CATEGORIAS;
  readonly ordenDecisiones: DecisionSegundaCita[] = ['AJUSTAR_Y_REVOTAR', 'CASAR_CON_CONDICIONES', 'VELORIO'];

  readonly citas = signal<SegundaCita[]>([]);
  readonly convocadas = computed(() => this.citas().filter((c) => c.estado === 'CONVOCADA'));
  readonly realizadas = computed(() => this.citas().filter((c) => c.estado === 'REALIZADA'));
  readonly sel = signal<SegundaCita | null>(null);
  readonly objeciones = signal<GrupoObjeciones[]>([]);
  readonly ocupado = signal(false);
  readonly error = signal<string | null>(null);
  f = { minuta: '', decision: null as DecisionSegundaCita | null, condiciones: '' };

  readonly segundos = signal(0);
  readonly corriendo = signal(false);
  private timer?: ReturnType<typeof setInterval>;

  readonly reloj = computed(() => {
    const s = this.segundos();
    return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
  });
  readonly indiceBloque = computed(() => {
    const min = this.segundos() / 60;
    const i = BLOQUES.findIndex(([d, h]) => min >= d && min < h);
    return i === -1 ? BLOQUES.length : i;
  });
  readonly bloqueActual = computed(() =>
    this.segundos() >= DURACION ? '¡Tiempo! A decidir' : this.segundos() === 0 ? 'Lista para empezar' : BLOQUES[this.indiceBloque()][2],
  );

  ngOnInit(): void {
    this.cargar();
  }

  ngOnDestroy(): void {
    clearInterval(this.timer);
  }

  private cargar(): void {
    this.api.segundasCitas(true).subscribe({ next: (c) => this.citas.set(c), error: (e) => this.ui.error(e) });
  }

  dia(iso: string): string {
    return String(Number(iso.slice(8, 10)));
  }

  mes(iso: string): string {
    return ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'][Number(iso.slice(5, 7)) - 1];
  }

  avanceBloque(i: number): number {
    const [d, h] = BLOQUES[i];
    const min = this.segundos() / 60;
    return Math.max(0, Math.min(100, ((min - d) / (h - d)) * 100));
  }

  abrir(c: SegundaCita): void {
    this.sel.set(c);
    this.reiniciar();
    this.error.set(null);
    this.f = { minuta: '', decision: null, condiciones: '' };
    this.objeciones.set([]);
    this.api.objeciones(c.procedimiento.id).subscribe({ next: (g) => this.objeciones.set(g), error: (e) => this.ui.error(e) });
  }

  cerrar(): void {
    this.reiniciar();
    this.sel.set(null);
  }

  alternarReloj(): void {
    if (this.corriendo()) {
      clearInterval(this.timer);
      this.corriendo.set(false);
      return;
    }
    this.corriendo.set(true);
    this.timer = setInterval(() => {
      this.segundos.update((s) => Math.min(DURACION, s + 1));
      if (this.segundos() >= DURACION) {
        clearInterval(this.timer);
        this.corriendo.set(false);
      }
    }, 1000);
  }

  reiniciar(): void {
    clearInterval(this.timer);
    this.corriendo.set(false);
    this.segundos.set(0);
  }

  realizar(lanzarRapida: boolean): void {
    const c = this.sel();
    if (!c) return;
    if (!this.f.minuta.trim()) return this.error.set('Sin minuta no hubo reunión: escribí qué se habló.');
    if (!this.f.decision) return this.error.set('Elegí una decisión.');
    if (this.f.decision === 'CASAR_CON_CONDICIONES' && !this.f.condiciones.trim())
      return this.error.set('Casar con condiciones… ¿cuáles? Escribilas.');
    this.error.set(null);
    this.ocupado.set(true);
    this.api
      .realizarCita(c.id, {
        minuta: this.f.minuta.trim(),
        decision: this.f.decision,
        condiciones: this.f.condiciones.trim() || null,
        lanzarRapida,
      })
      .subscribe({
        next: (r) => {
          this.ocupado.set(false);
          this.ui.ok(
            r.cicloRapido
              ? `Votación rápida abierta hasta el ${fDiaMes(r.cicloRapido.fechaFin)}. ¡Que voten!`
              : `${r.procedimiento.codigo} espera al próximo Tinder. El dueño ya puede ajustarlo.`,
          );
          this.ui.avisarCambio();
          this.cerrar();
          this.cargar();
        },
        error: (e) => {
          this.ocupado.set(false);
          this.error.set(mensajeDeError(e));
        },
      });
  }
}
