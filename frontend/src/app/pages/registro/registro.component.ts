import { Component, TemplateRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ESTADOS_CASAMIENTO, anio, fCorta, hoyIso, lista } from '../../core/diseno';
import { Casamiento, Procedimiento, Usuario, Velorio } from '../../core/models';
import { UiService } from '../../core/ui.service';

type Pestania = 'casamientos' | 'velorios' | 'aniversarios';

const ETIQUETAS: Record<Pestania, string> = { casamientos: 'Casamientos', velorios: 'Velorios', aniversarios: 'Aniversarios' };

@Component({
  selector: 'app-registro',
  imports: [FormsModule],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pagina">Registro civil</h1>
      <p class="subtitulo">Casamientos, velorios y aniversarios de procedimientos.</p>
      <div class="segmentado">
        @for (t of pestanias(); track t) {
          <button [class.on]="pestania() === t" (click)="pestania.set(t)">{{ etiquetas[t] }}</button>
        }
      </div>

      <!-- Casamientos -->
      @if (pestania() === 'casamientos') {
        @if (esCalidad() && pendCas().length) {
          <div class="rotulo">Esperando en el altar</div>
          <div class="lista" style="margin-bottom: 22px">
            @for (p of pendCas(); track p.id) {
              <div class="pendiente cas">
                <div>
                  <div class="codigo" style="color: #1D5A52">{{ p.codigo }} · match en {{ areasTxt(p) }}</div>
                  <div class="nombre-serif" style="font-size: 20px; margin-top: 2px">{{ p.nombre }}</div>
                </div>
                <button class="btn-chico btn-match" (click)="abrirCasar(p, hojaCasar)"><span class="ms">favorite</span>Registrar casamiento</button>
              </div>
            }
          </div>
        }
        <div class="rotulo">Libro de casamientos</div>
        <div class="lista">
          @for (c of casamientos(); track c.id) {
            <button class="tarjeta boda" (click)="ui.abrirDoc('libreta', c.id)">
              <div class="anillos" style="--d: 28px; --b: 2.5px; --c2: #2E7D73; width: 42px"></div>
              <div class="flex" style="min-width: 0">
                <div class="fila">
                  <span class="codigo">{{ c.codigo }}</span>
                  <span class="chip" [style.background]="ec(c)[1]" [style.color]="ec(c)[2]">{{ ec(c)[0] }}</span>
                </div>
                <div class="nombre-serif" style="margin-top: 3px">{{ c.nombre }}</div>
                <div class="meta">Casado el {{ fCorta(c.fecha) }} con {{ lista(c.areas) }}</div>
              </div>
              <span class="ms chevron">chevron_right</span>
            </button>
          } @empty {
            <div class="vacio-italico">Todavía no hubo casamientos.</div>
          }
        </div>
      }

      <!-- Velorios -->
      @if (pestania() === 'velorios') {
        @if (esCalidad() && pendVel().length) {
          <div class="rotulo">Pendientes de velorio</div>
          <div class="lista" style="margin-bottom: 22px">
            @for (p of pendVel(); track p.id) {
              <div class="pendiente vel">
                <div>
                  <div class="codigo">{{ p.codigo }} · rechazado</div>
                  <div class="nombre-serif" style="font-size: 20px; margin-top: 2px">{{ p.nombre }}</div>
                </div>
                <button class="btn-chico btn-luto" (click)="abrirVelar(p, hojaVelar)"><span class="ms">local_florist</span>Organizar velorio</button>
              </div>
            }
          </div>
        }
        <div class="rotulo">Necrológicas</div>
        <div class="lista">
          @for (v of velorios(); track v.id) {
            <button class="lapida" (click)="ui.abrirDoc('necro', v.id)">
              <div class="fila">
                <span class="cruz">✝</span>
                <span class="codigo" style="color: #9AA3A5">{{ v.codigo }} · {{ anio(v.fechaNacimiento) }} – {{ anio(v.fecha) }}</span>
                <span class="flex"></span>
                <span class="chip" [style.background]="v.actaFirmada ? 'rgba(159,211,203,.16)' : '#F2A541'" [style.color]="v.actaFirmada ? '#9FD3CB' : '#3D2600'">
                  {{ v.actaFirmada ? 'Acta firmada' : 'Falta firma' }}
                </span>
              </div>
              <div class="difunto">{{ v.nombre }}</div>
              <div class="epitafio">{{ v.epitafio }}</div>
            </button>
          } @empty {
            <div class="vacio-italico">Nadie murió todavía. Por ahora.</div>
          }
        </div>
      }

      <!-- Aniversarios -->
      @if (pestania() === 'aniversarios') {
        <p class="intro-aniv">Casamientos que cumplen 6 meses en los próximos 15 días. ¿Renuevan los votos o se divorcian?</p>
        <div class="lista" style="gap: 12px">
          @for (a of aniversarios(); track a.id) {
            <div class="tarjeta aniv">
              <div class="fila" style="align-items: flex-start; gap: 14px">
                <div class="caja-dias" [class.vencido]="a.diasParaAniversario < 0">
                  <div class="num">{{ a.diasParaAniversario === 0 ? '¡Hoy!' : abs(a.diasParaAniversario) }}</div>
                  <div class="unid">{{ a.diasParaAniversario > 0 ? 'días' : a.diasParaAniversario < 0 ? 'vencido' : '' }}</div>
                </div>
                <div class="flex" style="min-width: 0">
                  <div class="codigo">{{ a.codigo }}</div>
                  <div class="nombre-serif" style="margin-top: 2px">{{ a.nombre }}</div>
                  <div class="meta" style="margin-top: 4px; line-height: 1.4">{{ metaAniv(a) }}</div>
                  @if (a.obligatorio) {
                    <div class="aviso-oblig"><span class="ms">gavel</span>Es obligatorio: si se divorcian vuelve a segunda cita, no al velorio.</div>
                  }
                </div>
              </div>
              <div class="fila" style="gap: 8px">
                <button class="btn btn-match flex" style="height: 44px; border-radius: 12px" (click)="renovar(a)" [disabled]="ocupado()">Renovar votos</button>
                <button class="btn divorciar flex" (click)="divorciar(a)" [disabled]="ocupado()">Divorciar</button>
              </div>
            </div>
          } @empty {
            <div class="vacio-italico">No hay aniversarios cerca. Todo el mundo feliz.</div>
          }
        </div>
      }
    </div>

    <ng-template #hojaCasar>
      <div class="titulo-hoja">Registrar casamiento</div>
      <div class="form">
        <div class="campo">
          <span>Procedimiento</span>
          <div class="opciones">
            @for (p of pendCas(); track p.id) {
              <button class="pastilla chica" [class.on]="p.id === casarP()?.id" (click)="elegirCasar(p)">{{ p.codigo }} · {{ p.nombre }}</button>
            }
          </div>
        </div>
        @if (casarP(); as p) {
          @if (p.areas.length > 1) {
            <div class="grupal"><span class="ms">diversity_3</span>Casamiento grupal con {{ areasTxt(p) }}</div>
          }
        }
        <label class="campo"><span>Votos</span><textarea rows="3" class="poetico" [(ngModel)]="fc.votos"></textarea></label>
        <label class="campo"><span>Acuerdo prenupcial · versión express</span><textarea rows="4" [(ngModel)]="fc.express"></textarea></label>
        <label class="campo">
          <span>Padrino</span>
          <select [(ngModel)]="fc.padrinoId">
            <option [ngValue]="null">Sin padrino (nadie se hace cargo)</option>
            @for (u of usuarios(); track u.id) {
              <option [ngValue]="u.id">{{ u.nombre }} · {{ u.areaNombre }}</option>
            }
          </select>
        </label>
        <div class="dato-fijo">
          <span class="ms">verified</span>
          @if (oficiante(); as o) { Oficia: <strong>{{ o.nombre }}</strong> · Dirección } @else { Buscando oficiante en la Dirección… }
        </div>
        <button class="btn btn-match" (click)="casar()" [disabled]="ocupado() || !oficiante()">{{ ocupado() ? 'Casando…' : 'Casar y emitir libreta' }}</button>
      </div>
    </ng-template>

    <ng-template #hojaVelar>
      <div class="titulo-hoja">Organizar velorio</div>
      <div class="form">
        <div class="campo">
          <span>Difunto</span>
          <div class="opciones">
            @for (p of pendVel(); track p.id) {
              <button class="pastilla chica" [class.on]="p.id === fv.pid" (click)="elegirVelar(p)">{{ p.codigo }} · {{ p.nombre }}</button>
            }
          </div>
        </div>
        <label class="campo"><span>Causa de muerte</span><input [(ngModel)]="fv.causa" /></label>
        <label class="campo"><span>Epitafio</span><textarea rows="2" class="poetico" [(ngModel)]="fv.epitafio"></textarea></label>
        <label class="campo"><span>Deudos</span><input [(ngModel)]="fv.deudos" /></label>
        @if (fv.min !== null) {
          <div class="dato-fijo"><span class="ms">hourglass_bottom</span>Nos robaba <strong>{{ fv.min }} min por semana</strong></div>
        }
        <button class="btn btn-luto" (click)="velar()" [disabled]="ocupado()">{{ ocupado() ? 'Publicando…' : 'Publicar necrológica' }}</button>
      </div>
    </ng-template>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .fila { display: flex; align-items: center; gap: 8px; }
    .meta { font-size: 12.5px; color: var(--gris); margin-top: 3px; }
    .pendiente { padding: 16px; border-radius: 18px; display: flex; flex-direction: column; gap: 12px; }
    .pendiente.cas { background: #E3EFEC; }
    .pendiente.vel { background: #E6E2DA; }
    .boda { text-align: left; border: none; cursor: pointer; padding: 16px; display: flex; gap: 14px; align-items: center; width: 100%; }
    .chevron { font-size: 20px; color: var(--gris-claro); }
    .lapida {
      text-align: left; border: none; cursor: pointer; padding: 18px; border-radius: 18px; width: 100%;
      background: var(--luto); color: #E9E9E9; display: flex; flex-direction: column; gap: 8px;
    }
    .cruz { color: var(--ambar); font-size: 16px; }
    .difunto { font-family: var(--serif); font-size: 20px; color: #fff; }
    .epitafio { font-family: var(--serif); font-style: italic; font-size: 15px; line-height: 1.4; color: #F4E3C4; }
    .intro-aniv { margin: 0 0 14px; font-size: 13.5px; line-height: 1.45; color: var(--texto-2); }
    .aniv { padding: 16px; display: flex; flex-direction: column; gap: 14px; }
    .caja-dias { flex: none; width: 64px; padding: 8px 0; border-radius: 14px; background: #DCEDEA; color: #1D5A52; text-align: center; }
    .caja-dias.vencido { background: #F8E1DB; color: #8A2F1D; }
    .caja-dias .num { font-family: var(--serif); font-size: 28px; line-height: 1; }
    .caja-dias .unid { font-size: 10.5px; font-weight: 600; letter-spacing: .04em; margin-top: 2px; }
    .aviso-oblig { display: flex; gap: 6px; align-items: flex-start; margin-top: 8px; font-size: 12px; line-height: 1.4; color: #7A4A0A; }
    .aviso-oblig .ms { font-size: 15px; }
    .divorciar { height: 44px; border-radius: 12px; border: 1px solid rgba(208,87,63,.5); background: transparent; color: var(--rechazo-txt); }
    .opciones { display: flex; flex-wrap: wrap; gap: 6px; }
    .grupal { display: flex; gap: 8px; align-items: center; padding: 10px 12px; border-radius: 12px; background: #FBE9CC; color: #5C3A05; font-size: 13px; }
    .grupal .ms { font-size: 18px; }
  `,
})
export class RegistroComponent {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  readonly ui = inject(UiService);

  readonly fCorta = fCorta;
  readonly lista = lista;
  readonly anio = anio;
  readonly abs = Math.abs;
  readonly etiquetas = ETIQUETAS;

  readonly rol = computed(() => this.auth.usuario()?.rol);
  readonly esCalidad = computed(() => this.rol() === 'CALIDAD');
  readonly pestanias = computed<Pestania[]>(() =>
    this.rol() === 'EMPLEADO' ? ['casamientos', 'velorios'] : ['casamientos', 'velorios', 'aniversarios'],
  );
  readonly pestania = signal<Pestania>('casamientos');

  readonly casamientos = signal<Casamiento[]>([]);
  readonly velorios = signal<Velorio[]>([]);
  readonly aniversarios = signal<Casamiento[]>([]);
  readonly pendCas = signal<Procedimiento[]>([]);
  private readonly pendVelTodos = signal<Procedimiento[]>([]);
  readonly pendVel = computed(() => {
    const velados = new Set(this.velorios().map((v) => v.procedimientoId));
    return this.pendVelTodos().filter((p) => !velados.has(p.id));
  });
  readonly usuarios = signal<Usuario[]>([]);
  readonly oficiante = signal<Usuario | null>(null);
  readonly ocupado = signal(false);

  readonly casarP = signal<Procedimiento | null>(null);
  fc = { votos: '', express: '', padrinoId: null as number | null };
  fv = { pid: 0, causa: '', epitafio: '', deudos: '', min: null as number | null };

  constructor() {
    // Recarga al entrar y cada vez que otra parte de la app cambia datos (p. ej. firmar un acta).
    effect(() => {
      this.ui.cambios();
      untracked(() => this.cargar());
    });
    if (this.esCalidad()) {
      this.api.usuarios().subscribe((u) => this.usuarios.set(u));
      this.api.usuarios('DIRECCION').subscribe((o) => this.oficiante.set(o[0] ?? null));
    }
  }

  private cargar(): void {
    const err = (e: unknown) => this.ui.error(e);
    this.api.casamientos().subscribe({ next: (c) => this.casamientos.set(c), error: err });
    this.api.velorios().subscribe({ next: (v) => this.velorios.set(v), error: err });
    if (this.rol() !== 'EMPLEADO') {
      this.api.aniversarios().subscribe({ next: (a) => this.aniversarios.set(a), error: err });
    }
    if (this.esCalidad()) {
      this.api.procedimientos('PENDIENTE_CASAMIENTO').subscribe({ next: (p) => this.pendCas.set(p), error: err });
      this.api.procedimientos('PENDIENTE_VELORIO').subscribe({ next: (p) => this.pendVelTodos.set(p), error: err });
    }
  }

  areasTxt(p: Procedimiento): string {
    return lista(p.areas.map((a) => a.nombre));
  }

  ec(c: Casamiento) {
    return ESTADOS_CASAMIENTO[c.estado];
  }

  metaAniv(c: Casamiento): string {
    const n = c.diasParaAniversario;
    const cuando = n < 0 ? `Aniversario vencido hace ${-n} días.` : `Aniversario el ${fCorta(c.fechaAniversario)}.`;
    return `Casado el ${fCorta(c.fecha)} con ${lista(c.areas)}. ${cuando}`;
  }

  // ---------- Casar ----------

  abrirCasar(p: Procedimiento, hoja: TemplateRef<unknown>): void {
    this.elegirCasar(p);
    this.ui.abrirHoja(hoja);
  }

  elegirCasar(p: Procedimiento): void {
    this.casarP.set(p);
    this.fc = {
      votos: 'Prometemos usarte en las buenas, en las malas y en las auditorías.',
      express: p.versionExpress ?? '1. …\n2. …\n3. …',
      padrinoId: p.autorId,
    };
  }

  casar(): void {
    const p = this.casarP();
    const o = this.oficiante();
    if (!p || !o) return;
    this.ocupado.set(true);
    this.api
      .casar({
        procedimientoId: p.id,
        fecha: hoyIso(),
        votos: this.fc.votos,
        versionExpress: this.fc.express,
        oficianteId: o.id,
        padrinoId: this.fc.padrinoId,
        areaIds: p.areas.map((a) => a.id),
      })
      .subscribe({
        next: (c) => {
          this.ocupado.set(false);
          this.ui.abrirDoc('libreta', c.id);
          this.ui.ok(`${c.codigo} y su equipo: felices para siempre`);
          this.ui.avisarCambio();
        },
        error: (e) => {
          this.ocupado.set(false);
          this.ui.error(e);
        },
      });
  }

  // ---------- Velar ----------

  abrirVelar(p: Procedimiento, hoja: TemplateRef<unknown>): void {
    this.elegirVelar(p);
    this.ui.abrirHoja(hoja);
  }

  /** Trae causa, epitafio y deudos sugeridos por el backend (editables). */
  elegirVelar(p: Procedimiento): void {
    this.fv = { pid: p.id, causa: '', epitafio: '', deudos: '', min: p.minutosSemanales };
    this.api.sugerenciaVelorio(p.id).subscribe({
      next: (s) => {
        if (this.fv.pid !== p.id) return;
        this.fv = { pid: p.id, causa: s.causaMuerte, epitafio: s.epitafio, deudos: s.deudos, min: s.minutosSemanalesPerdidos };
      },
      error: (e) => this.ui.error(e),
    });
  }

  velar(): void {
    const f = this.fv;
    if (!f.pid) return;
    this.ocupado.set(true);
    this.api.crearVelorio({ procedimientoId: f.pid, fecha: hoyIso(), causaMuerte: f.causa, epitafio: f.epitafio, deudos: f.deudos }).subscribe({
      next: (v) => {
        this.ocupado.set(false);
        this.ui.abrirDoc('necro', v.id);
        this.ui.ok('Necrológica publicada. Falta la firma de la Dirección.');
        this.ui.avisarCambio();
      },
      error: (e) => {
        this.ocupado.set(false);
        this.ui.error(e);
      },
    });
  }

  // ---------- Aniversarios ----------

  renovar(c: Casamiento): void {
    this.ocupado.set(true);
    this.api.renovar(c.id).subscribe({
      next: () => {
        this.ocupado.set(false);
        this.ui.ok('Votos renovados por 6 meses más');
        this.ui.avisarCambio();
      },
      error: (e) => {
        this.ocupado.set(false);
        this.ui.error(e);
      },
    });
  }

  divorciar(c: Casamiento): void {
    this.ocupado.set(true);
    this.api.divorciar(c.id).subscribe({
      next: () => {
        this.ocupado.set(false);
        this.ui.ok(c.obligatorio ? `${c.codigo} vuelve a segunda cita` : `${c.codigo} pasa a pendiente de velorio`);
        this.ui.avisarCambio();
      },
      error: (e) => {
        this.ocupado.set(false);
        this.ui.error(e);
      },
    });
  }
}
