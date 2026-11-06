import { Component, OnInit, TemplateRef, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import {
  ALERTAS,
  ESTADOS,
  ESTADOS_LISTA,
  diasEntre,
  fRangoCorto,
  fRangoLargo,
  hoyIso,
  iconoTipo,
  siguienteTrimestre,
} from '../../core/diseno';
import { Alerta, Area, Ciclo, EstadoProcedimiento, Procedimiento, ResumenCierre, TipoProcedimiento } from '../../core/models';
import { UiService, mensajeDeError } from '../../core/ui.service';

type Filtro = EstadoProcedimiento | 'TODOS';

interface FormProc {
  id: number | null;
  tipo: TipoProcedimiento;
  codigo: string;
  nombre: string;
  bio: string;
  min: number;
  usos: number;
  beneficio: string;
  oblig: boolean;
  base: string;
  areaIds: number[];
  fechaCreacion: string;
  autorId: number | null;
  versionExpress: string | null;
}

@Component({
  selector: 'app-gestion',
  imports: [FormsModule],
  template: `
    <div class="pantalla">
      <div class="segmentado">
        <button [class.on]="pestania() === 'procedimientos'" (click)="pestania.set('procedimientos')">Procedimientos</button>
        <button [class.on]="pestania() === 'ciclos'" (click)="pestania.set('ciclos')">Ciclos</button>
      </div>

      @if (pestania() === 'procedimientos') {
        <div class="fila" style="margin-bottom: 14px">
          <h1 class="titulo-pagina flex" style="font-size: 28px">Procedimientos</h1>
          <button class="btn-chico btn-ambar" style="padding: 0 14px 0 10px; gap: 4px" (click)="nuevo(hojaProc)">
            <span class="ms" style="font-size: 20px">add</span>Nuevo
          </button>
        </div>
        <div class="pastillas">
          @for (f of filtros; track f) {
            <button class="pastilla" style="height: 34px; padding: 0 12px; font-size: 13px" [class.on]="filtro() === f" (click)="filtro.set(f)">
              {{ f === 'TODOS' ? 'Todos' : estados[f][0] }}<span class="n">{{ conteo()[f] }}</span>
            </button>
          }
        </div>
        <div class="grilla">
          @for (p of visibles(); track p.id) {
            <button class="tarjeta proc" (click)="editar(p, hojaProc)">
              <div class="fila" style="gap: 6px"><span class="codigo flex">{{ p.codigo }}</span><span class="ms tipo">{{ iconoTipo(p.tipo) }}</span></div>
              <div class="nombre-proc flex">{{ p.nombre }}</div>
              <span class="chip" style="align-self: flex-start" [style.background]="estados[p.estado][1]" [style.color]="estados[p.estado][2]">{{ estados[p.estado][0] }}</span>
              <div class="fila meta" style="gap: 4px">
                <span class="flex">{{ p.minutosPorUso }} min · {{ p.areas.length }} {{ p.areas.length === 1 ? 'área' : 'áreas' }}</span>
                @if (p.obligatorio) { <span class="ms" style="font-size: 15px; color: #9A5B12" title="Obligatorio por norma">gavel</span> }
              </div>
            </button>
          } @empty {
            <div class="vacio-italico" style="grid-column: 1 / -1">Nada por acá.</div>
          }
        </div>
      }

      @if (pestania() === 'ciclos') {
        @if (ui.ciclo(); as c) {
          <div class="ciclo-abierto">
            <span class="estado"><span class="punto"></span>Ciclo abierto</span>
            <div>
              <div class="ciclo-nombre">{{ c.nombre }}</div>
              <div class="ciclo-rango">{{ rangoLargo(c) }} · quedan {{ quedan(c) }} días</div>
            </div>
            <div>
              <div class="pista"><div [style.width.%]="avance(c).pct"></div></div>
              <div class="ciclo-rango" style="margin-top: 6px">Día {{ avance(c).dia }} de {{ avance(c).total }} del trimestre</div>
            </div>
            <button class="btn btn-ambar" style="height: 48px" (click)="ui.abrirHoja(hojaCerrar)">Cerrar ciclo y calcular resultados</button>
          </div>
        } @else if (ui.ciclo() === null) {
          <div class="ciclo-cerrado">
            <div class="serif" style="font-size: 24px; color: var(--petroleo)">No hay ciclo abierto</div>
            <div class="nota">Un solo ciclo a la vez. Al abrirlo, los procedimientos en Tinder y en segunda cita vuelven al mazo.</div>
            <button class="btn btn-petroleo" style="height: 48px" (click)="abrirCiclo()" [disabled]="ocupado()">
              {{ ocupado() ? 'Abriendo…' : 'Abrir ciclo ' + proximo().nombre }}
            </button>
          </div>
        }

        @if (resumen(); as r) {
          <div class="rotulo" style="margin-top: 22px">Cierre de {{ r.ciclo.nombre }} · {{ r.resultadosCalculados }} resultados por área</div>
          <div class="tarjeta tabla">
            @for (t of r.transiciones; track t.procedimientoId) {
              <div class="renglon" style="gap: 7px">
                <div class="fila" style="align-items: baseline"><span class="codigo">{{ t.codigo }}</span><span class="serif" style="font-size: 16.5px; color: var(--petroleo)">{{ t.nombre }}</span></div>
                <div class="fila" style="gap: 6px; flex-wrap: wrap">
                  <span class="chip" [style.background]="estados[t.desde][1]" [style.color]="estados[t.desde][2]">{{ estados[t.desde][0] }}</span>
                  <span class="ms" style="font-size: 16px; color: var(--gris-claro)">arrow_forward</span>
                  <span class="chip" [style.background]="estados[t.hacia][1]" [style.color]="estados[t.hacia][2]">{{ estados[t.hacia][0] }}</span>
                </div>
                <div class="meta">{{ t.mensaje }}</div>
              </div>
            } @empty {
              <div class="renglon meta">Ningún procedimiento cambió de estado.</div>
            }
          </div>
        }

        <div class="rotulo" style="margin: 26px 0 10px">Alertas para Calidad</div>
        <div class="lista">
          @for (a of alertas(); track a.id) {
            <div class="tarjeta alerta" [style.opacity]="a.revisada ? .6 : 1">
              <span class="ms icono" [style.background]="alerta(a)[2]" [style.color]="alerta(a)[3]">{{ alerta(a)[1] }}</span>
              <div class="flex" style="min-width: 0; display: flex; flex-direction: column; gap: 3px">
                <div class="alerta-titulo">{{ alerta(a)[0] }}</div>
                <div class="codigo">{{ a.codigo }} · ciclo {{ a.cicloNombre }}</div>
                <div class="alerta-msg">{{ a.mensaje }}</div>
                @if (a.revisada) {
                  <span class="revisada"><span class="ms" style="font-size: 15px">check</span>Revisada</span>
                } @else {
                  <button class="marcar" (click)="revisar(a)">Marcar revisada</button>
                }
              </div>
            </div>
          } @empty {
            <div class="meta">Sin alertas. Calidad puede tomarse un café.</div>
          }
        </div>

        <div class="rotulo" style="margin: 26px 0 10px">Historial</div>
        <div class="tarjeta tabla">
          @for (h of historial(); track h.id) {
            <div class="renglon fila" style="gap: 10px; padding: 13px 16px; font-size: 13.5px">
              <strong class="hist-nombre">{{ h.nombre }}</strong>
              <span class="flex" style="color: var(--gris)">{{ rangoCorto(h) }}</span>
              <span class="chip" [style.background]="h.estado === 'ABIERTO' ? '#2E7D73' : '#E3E1DC'" [style.color]="h.estado === 'ABIERTO' ? '#fff' : '#4F5958'">
                {{ h.estado === 'ABIERTO' ? 'Abierto' : 'Cerrado' }}
              </span>
            </div>
          }
        </div>
      }
    </div>

    <ng-template #hojaCerrar>
      @if (ui.ciclo(); as c) {
        <div class="titulo-hoja">¿Cerramos el trimestre?</div>
        <p class="cerrar-txt">
          Se cuentan los votos de {{ c.nombre }} por área y cada procedimiento sigue su destino: casamiento, velorio o segunda cita.
          No se puede deshacer.
        </p>
        <button class="btn btn-petroleo" style="width: 100%; height: 50px" (click)="cerrarCiclo(c)" [disabled]="ocupado()">
          {{ ocupado() ? 'Contando votos…' : 'Sí, contar votos' }}
        </button>
        <button class="btn-texto" style="width: 100%; margin-top: 6px" (click)="ui.cerrarHoja()">Todavía no</button>
      }
    </ng-template>

    <ng-template #hojaProc>
      <div class="titulo-hoja">{{ fp.id ? 'Editar procedimiento' : 'Nuevo procedimiento' }}</div>
      <p class="meta" style="margin: 4px 0 0; font-size: 13px">Escribí su perfil como si buscara pareja.</p>
      <div class="form">
        <div class="segmentado" style="margin: 0; border-radius: 12px">
          <button style="height: 34px; border-radius: 9px" [class.on]="fp.tipo === 'PROCEDIMIENTO'" (click)="fp.tipo = 'PROCEDIMIENTO'">Procedimiento</button>
          <button style="height: 34px; border-radius: 9px" [class.on]="fp.tipo === 'REGISTRO'" (click)="fp.tipo = 'REGISTRO'">Registro</button>
        </div>
        <div class="dos" style="grid-template-columns: 110px minmax(0, 1fr)">
          <label class="campo"><span>Código</span><input class="mono" style="padding: 12px; font-size: 14px" [(ngModel)]="fp.codigo" placeholder="PR-40" maxlength="20" /></label>
          <label class="campo"><span>Nombre</span><input [(ngModel)]="fp.nombre" placeholder="Archivo de expedientes" /></label>
        </div>
        <label class="campo"><span>Bio (en primera persona)</span><textarea rows="3" class="poetico" [(ngModel)]="fp.bio" placeholder="Soy ordenado, me gustan los biblioratos y…"></textarea></label>
        <div class="dos">
          <label class="campo"><span>Min. por uso</span><input type="number" min="0" [(ngModel)]="fp.min" /></label>
          <label class="campo"><span>Usos por semana</span><input type="number" min="0" step="1" [(ngModel)]="fp.usos" /></label>
        </div>
        <label class="campo"><span>Te doy (beneficio)</span><input [(ngModel)]="fp.beneficio" placeholder="Encontrar cualquier expediente en 2 minutos" /></label>
        <div class="campo">
          <span>Áreas que lo usan</span>
          <div class="opciones">
            @for (a of areas(); track a.id) {
              <button class="pastilla chica" [class.on]="fp.areaIds.includes(a.id)" (click)="alternarArea(a.id)">{{ a.nombre }}</button>
            }
          </div>
        </div>
        <button class="toggle" (click)="fp.oblig = !fp.oblig" role="switch" [attr.aria-checked]="fp.oblig">
          <span class="flex" style="display: flex; flex-direction: column; gap: 2px">
            <span style="font-weight: 600; font-size: 14px; color: var(--petroleo)">Obligatorio por norma</span>
            <span style="font-size: 12px; color: var(--gris)">No se puede velar: si lo rechazan, va a segunda cita.</span>
          </span>
          <span class="interruptor" [class.on]="fp.oblig"><span></span></span>
        </button>
        @if (fp.oblig) {
          <label class="campo"><span>Base normativa</span><input [(ngModel)]="fp.base" placeholder="ISO 9001 7.5.3" /></label>
        }
        @if (errorProc()) {
          <div class="error-form">{{ errorProc() }}</div>
        }
        <button class="btn btn-petroleo" (click)="guardarProc()" [disabled]="ocupado()">
          {{ ocupado() ? 'Guardando…' : fp.id ? 'Guardar cambios' : 'Publicar en Tinder' }}
        </button>
        @if (fp.id) {
          <button class="btn-texto" style="color: var(--rechazo-txt)" (click)="borrarProc()" [disabled]="ocupado()">
            {{ confirmarBorrado() ? '¿Seguro? Tocá de nuevo para borrarlo' : 'Borrar procedimiento' }}
          </button>
        }
      </div>
    </ng-template>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .fila { display: flex; align-items: center; gap: 10px; }
    .meta { font-size: 12.5px; color: var(--gris); }
    .nota { font-size: 13.5px; color: var(--gris); line-height: 1.45; }
    .pastilla .n { font-size: 11.5px; opacity: .7; }
    .grilla { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
    .proc { text-align: left; border: none; cursor: pointer; padding: 14px; display: flex; flex-direction: column; gap: 8px; min-height: 150px; }
    .proc .tipo { font-size: 16px; color: var(--gris-claro); }
    .nombre-proc { font-family: var(--serif); font-size: 16.5px; line-height: 1.2; color: var(--petroleo); }
    .proc .meta { font-size: 11.5px; }

    .ciclo-abierto {
      padding: 20px; border-radius: 24px; color: var(--crema); display: flex; flex-direction: column; gap: 14px;
      background-color: var(--petroleo); background-image: repeating-linear-gradient(135deg, rgba(255,255,255,.035) 0 1px, transparent 1px 9px);
    }
    .estado {
      align-self: flex-start; display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; border-radius: 999px;
      background: rgba(46,125,115,.35); font-size: 11.5px; font-weight: 600; letter-spacing: .06em; text-transform: uppercase;
    }
    .estado .punto { width: 7px; height: 7px; border-radius: 50%; background: #6FD0C2; }
    .ciclo-nombre { font-family: var(--serif); font-size: 48px; line-height: .95; letter-spacing: -.02em; }
    .ciclo-rango { font-size: 13.5px; color: rgba(245,240,230,.75); margin-top: 6px; }
    .pista { height: 6px; border-radius: 4px; background: rgba(245,240,230,.15); overflow: hidden; }
    .pista > div { height: 100%; background: var(--ambar); border-radius: 4px; }
    .ciclo-cerrado { padding: 20px; border-radius: 24px; border: 1.5px dashed rgba(18,52,59,.22); display: flex; flex-direction: column; gap: 12px; }

    .tabla { overflow: hidden; }
    .renglon { padding: 14px 16px; display: flex; flex-direction: column; }
    .renglon + .renglon { border-top: 1px solid rgba(18,52,59,.07); }
    .renglon.fila { flex-direction: row; }
    .hist-nombre { font-weight: 600; color: var(--petroleo); width: 64px; }

    .alerta { padding: 14px; display: flex; gap: 12px; }
    .alerta .icono { flex: none; width: 38px; height: 38px; border-radius: 12px; display: grid; place-items: center; font-size: 20px; }
    .alerta-titulo { font-weight: 600; font-size: 14px; color: var(--petroleo); }
    .alerta-msg { font-size: 13px; line-height: 1.45; color: var(--texto-2); margin-top: 2px; }
    .marcar { align-self: flex-start; margin-top: 6px; padding: 0; border: none; background: none; color: var(--match); font-weight: 600; font-size: 13px; cursor: pointer; }
    .revisada { margin-top: 6px; font-size: 12.5px; color: var(--gris); display: flex; align-items: center; gap: 4px; }

    .cerrar-txt { margin: 10px 0 20px; font-size: 14px; line-height: 1.5; color: var(--texto-2); }
    .dos { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
    .opciones { display: flex; flex-wrap: wrap; gap: 6px; }
    .toggle {
      display: flex; align-items: center; gap: 12px; padding: 12px 14px; border-radius: 12px;
      border: 1px solid rgba(18,52,59,.12); background: #fff; cursor: pointer; text-align: left;
    }
    .interruptor { position: relative; flex: none; width: 44px; height: 26px; border-radius: 13px; background: rgba(18,52,59,.2); transition: background .2s; }
    .interruptor span {
      position: absolute; top: 3px; left: 3px; width: 20px; height: 20px; border-radius: 50%; background: #fff;
      box-shadow: 0 1px 3px rgba(0,0,0,.2); transition: transform .2s;
    }
    .interruptor.on { background: var(--match); }
    .interruptor.on span { transform: translateX(18px); }
  `,
})
export class GestionComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  readonly ui = inject(UiService);

  readonly estados = ESTADOS;
  readonly filtros: Filtro[] = ['TODOS', ...ESTADOS_LISTA];
  readonly iconoTipo = iconoTipo;

  readonly pestania = signal<'procedimientos' | 'ciclos'>('procedimientos');
  readonly procedimientos = signal<Procedimiento[]>([]);
  readonly filtro = signal<Filtro>('TODOS');
  readonly areas = signal<Area[]>([]);
  readonly ciclos = signal<Ciclo[]>([]);
  readonly alertas = signal<Alerta[]>([]);
  readonly resumen = signal<ResumenCierre | null>(null);
  readonly ocupado = signal(false);
  readonly errorProc = signal<string | null>(null);
  readonly confirmarBorrado = signal(false);
  fp: FormProc = vacio(null);

  readonly visibles = computed(() => {
    const f = this.filtro();
    return this.procedimientos().filter((p) => f === 'TODOS' || p.estado === f);
  });
  readonly conteo = computed(() => {
    const c = { TODOS: this.procedimientos().length } as Record<Filtro, number>;
    for (const e of ESTADOS_LISTA) c[e] = this.procedimientos().filter((p) => p.estado === e).length;
    return c;
  });
  readonly historial = computed(() => [...this.ciclos()].sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio)));
  readonly proximo = computed(() => {
    const fin = this.ciclos().reduce<string | null>((m, c) => (!m || c.fechaFin > m ? c.fechaFin : m), null);
    return siguienteTrimestre(fin);
  });

  ngOnInit(): void {
    this.cargarProcedimientos();
    this.cargarCiclos();
    this.api.areas().subscribe((a) => this.areas.set(a));
  }

  private cargarProcedimientos(): void {
    this.api.procedimientos().subscribe({ next: (p) => this.procedimientos.set(p), error: (e) => this.ui.error(e) });
  }

  private cargarCiclos(): void {
    this.api.ciclos().subscribe({ next: (c) => this.ciclos.set(c), error: (e) => this.ui.error(e) });
    this.api.alertas().subscribe({ next: (a) => this.alertas.set(a), error: (e) => this.ui.error(e) });
  }

  // ---------- Ciclos ----------

  rangoLargo(c: Ciclo): string {
    return fRangoLargo(c.fechaInicio, c.fechaFin);
  }

  rangoCorto(c: Ciclo): string {
    return fRangoCorto(c.fechaInicio, c.fechaFin);
  }

  quedan(c: Ciclo): number {
    return Math.max(0, diasEntre(hoyIso(), c.fechaFin));
  }

  avance(c: Ciclo): { dia: number; total: number; pct: number } {
    const total = diasEntre(c.fechaInicio, c.fechaFin) + 1;
    const dia = Math.min(total, Math.max(1, diasEntre(c.fechaInicio, hoyIso()) + 1));
    return { dia, total, pct: Math.round((dia / total) * 100) };
  }

  alerta(a: Alerta) {
    return ALERTAS[a.tipo];
  }

  cerrarCiclo(c: Ciclo): void {
    this.ocupado.set(true);
    this.api.cerrarCiclo(c.id).subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.ui.cerrarHoja();
        this.resumen.set(r);
        this.ui.ok(`Ciclo cerrado: ${r.resultadosCalculados} resultados por área calculados`);
        this.ui.recargarCiclo();
        this.ui.avisarCambio();
        this.cargarCiclos();
        this.cargarProcedimientos();
      },
      error: (e) => {
        this.ocupado.set(false);
        this.ui.error(e);
      },
    });
  }

  abrirCiclo(): void {
    this.ocupado.set(true);
    this.api.abrirCiclo(this.proximo()).subscribe({
      next: (c) => {
        this.ocupado.set(false);
        this.resumen.set(null);
        this.ui.ok(`Ciclo ${c.nombre} abierto. ¡A deslizar!`);
        this.ui.recargarCiclo();
        this.cargarCiclos();
        this.cargarProcedimientos();
      },
      error: (e) => {
        this.ocupado.set(false);
        this.ui.error(e);
      },
    });
  }

  revisar(a: Alerta): void {
    this.api.revisarAlerta(a.id).subscribe({
      next: (r) => this.alertas.update((l) => l.map((x) => (x.id === r.id ? r : x))),
      error: (e) => this.ui.error(e),
    });
  }

  // ---------- Procedimientos ----------

  nuevo(hoja: TemplateRef<unknown>): void {
    this.fp = vacio(this.auth.usuario()?.id ?? null);
    this.errorProc.set(null);
    this.confirmarBorrado.set(false);
    this.ui.abrirHoja(hoja);
  }

  editar(p: Procedimiento, hoja: TemplateRef<unknown>): void {
    this.fp = {
      id: p.id,
      tipo: p.tipo,
      codigo: p.codigo,
      nombre: p.nombre,
      bio: p.bio,
      min: p.minutosPorUso,
      usos: p.usosPorSemana,
      beneficio: p.beneficio,
      oblig: p.obligatorio,
      base: p.baseObligatoriedad ?? '',
      areaIds: p.areas.map((a) => a.id),
      fechaCreacion: p.fechaCreacion,
      autorId: p.autorId,
      versionExpress: p.versionExpress,
    };
    this.errorProc.set(null);
    this.confirmarBorrado.set(false);
    this.ui.abrirHoja(hoja);
  }

  alternarArea(id: number): void {
    const ids = this.fp.areaIds;
    this.fp.areaIds = ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id];
  }

  guardarProc(): void {
    const f = this.fp;
    if (!f.codigo.trim() || !f.nombre.trim()) {
      this.errorProc.set('Falta código y nombre: nadie hace match con un perfil vacío.');
      return;
    }
    if (!f.areaIds.length) {
      this.errorProc.set('Elegí al menos un área: alguien lo tiene que usar.');
      return;
    }
    const body = {
      codigo: f.codigo.trim().toUpperCase(),
      nombre: f.nombre.trim(),
      tipo: f.tipo,
      bio: f.bio.trim() || 'Recién llegado. Todavía no sé bien qué hago acá.',
      fechaCreacion: f.fechaCreacion,
      minutosPorUso: Math.max(0, Math.round(Number(f.min) || 0)),
      usosPorSemana: Math.max(0, Math.round(Number(f.usos) || 0)),
      beneficio: f.beneficio.trim() || 'Por definir',
      obligatorio: f.oblig,
      baseObligatoriedad: f.oblig ? f.base.trim() || null : null,
      autorId: f.autorId,
      versionExpress: f.versionExpress,
      areaIds: f.areaIds,
    };
    this.ocupado.set(true);
    this.errorProc.set(null);
    const op = f.id ? this.api.actualizarProcedimiento(f.id, body) : this.api.crearProcedimiento(body);
    op.subscribe({
      next: (p) => {
        this.ocupado.set(false);
        this.ui.cerrarHoja();
        this.ui.ok(f.id ? `Perfil de ${p.codigo} actualizado` : `${p.codigo} ya está en Tinder`);
        if (!f.id) this.filtro.set('TODOS');
        this.cargarProcedimientos();
      },
      error: (e) => {
        this.ocupado.set(false);
        this.errorProc.set(mensajeDeError(e));
      },
    });
  }

  borrarProc(): void {
    const id = this.fp.id;
    if (!id) return;
    if (!this.confirmarBorrado()) {
      this.confirmarBorrado.set(true);
      return;
    }
    this.ocupado.set(true);
    this.api.eliminarProcedimiento(id).subscribe({
      next: () => {
        this.ocupado.set(false);
        this.ui.cerrarHoja();
        this.ui.ok(`${this.fp.codigo} borrado`);
        this.cargarProcedimientos();
      },
      error: (e) => {
        this.ocupado.set(false);
        this.confirmarBorrado.set(false);
        this.errorProc.set(mensajeDeError(e));
      },
    });
  }
}

function vacio(autorId: number | null): FormProc {
  return {
    id: null,
    tipo: 'PROCEDIMIENTO',
    codigo: '',
    nombre: '',
    bio: '',
    min: 10,
    usos: 1,
    beneficio: '',
    oblig: false,
    base: '',
    areaIds: [],
    fechaCreacion: hoyIso(),
    autorId,
    versionExpress: null,
  };
}
