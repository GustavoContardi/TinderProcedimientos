import { Component, OnInit, TemplateRef, computed, inject, input, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ESTADOS_MEJORA, ESTADOS_MEJORA_LISTA, ORIGENES_MEJORA, ORIGENES_MEJORA_LISTA, fCorta } from '../../core/diseno';
import { EstadoMejora, Mejora, OrigenMejora, Usuario } from '../../core/models';
import { UiService, mensajeDeError } from '../../core/ui.service';

/** Tablero de mejoras: kanban por estado. Lo ven todos; editan el juez de paz y el responsable. */
@Component({
  selector: 'app-tablero',
  imports: [FormsModule],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pagina">Tablero de mejoras</h1>
      <p class="subtitulo">Toda objeción, acuerdo y acta termina acá, con responsable y fecha.</p>

      <div class="filtros">
        <select [ngModel]="filtroProc()" (ngModelChange)="filtroProc.set($event)">
          <option [ngValue]="null">Todos los procedimientos</option>
          @for (p of procedimientos(); track p) { <option [ngValue]="p">{{ p }}</option> }
        </select>
      </div>
      <div class="pastillas">
        <button class="pastilla" [class.on]="filtroOrigen() === null" (click)="filtroOrigen.set(null)">Todos los orígenes</button>
        @for (o of origenes; track o) {
          <button class="pastilla" [class.on]="filtroOrigen() === o" (click)="filtroOrigen.set(o)">
            <span class="ms" style="font-size: 16px">{{ orig[o][1] }}</span>{{ orig[o][0] }}
          </button>
        }
      </div>

      <div class="kanban">
        @for (col of columnas; track col) {
          <div class="columna">
            <div class="col-cab">
              <span class="chip" [style.background]="est[col][1]" [style.color]="est[col][2]">{{ est[col][0] }}</span>
              <span class="n">{{ porEstado()[col].length }}</span>
            </div>
            @for (m of porEstado()[col]; track m.id) {
              <button class="tarjeta mejora" [class.vencida]="m.vencida" (click)="abrir(m)">
                <div class="fila">
                  <span class="codigo">{{ m.codigo }} · {{ m.procedimientoCodigo }}</span>
                  <span class="flex"></span>
                  <span class="ms origen" [title]="orig[m.origen][0]">{{ orig[m.origen][1] }}</span>
                </div>
                <div class="desc">{{ m.descripcion }}</div>
                <div class="fila meta">
                  <span class="ms" style="font-size: 15px">person</span>{{ m.responsableNombre }}
                </div>
                <div class="fila meta" [class.rojo]="m.vencida">
                  <span class="ms" style="font-size: 15px">event</span>{{ fCorta(m.fechaCompromiso) }}{{ m.vencida ? ' · vencida' : '' }}
                </div>
              </button>
            } @empty {
              <div class="vacia">{{ vacias[col] }}</div>
            }
          </div>
        }
      </div>
    </div>

    <ng-template #hojaMejora>
      @if (sel(); as m) {
        <div class="titulo-hoja">{{ m.codigo }} · {{ m.procedimientoCodigo }}</div>
        <p class="meta" style="margin: 4px 0 0">{{ m.procedimientoNombre }} · origen: {{ orig[m.origen][0] }}</p>
        @if (puedeEditar(m)) {
          <div class="form">
            <label class="campo"><span>Descripción</span><textarea rows="3" [(ngModel)]="fm.descripcion"></textarea></label>
            <div class="dos">
              <label class="campo">
                <span>Responsable</span>
                @if (esJuez()) {
                  <select [(ngModel)]="fm.responsableId">
                    @for (u of usuarios(); track u.id) { <option [ngValue]="u.id">{{ u.nombre }}</option> }
                  </select>
                } @else {
                  <input [value]="m.responsableNombre" disabled />
                }
              </label>
              <label class="campo"><span>Fecha compromiso</span><input type="date" [(ngModel)]="fm.fechaCompromiso" /></label>
            </div>
            <div class="campo">
              <span>Estado</span>
              <div class="opciones">
                @for (e of columnas; track e) {
                  <button class="pastilla chica" [class.on]="fm.estado === e" (click)="fm.estado = e">{{ est[e][0] }}</button>
                }
              </div>
            </div>
            <label class="campo">
              <span>Respuesta {{ cierra() ? '(obligatoria para cerrar)' : '(opcional)' }}</span>
              <textarea rows="3" [(ngModel)]="fm.respuesta" placeholder="¿Qué se hizo? ¿Por qué se descarta?"></textarea>
            </label>
            @if (error()) { <div class="error-form">{{ error() }}</div> }
            <button class="btn btn-petroleo" (click)="guardar()" [disabled]="ocupado()">{{ ocupado() ? 'Guardando…' : 'Guardar' }}</button>
          </div>
        } @else {
          <div class="solo-lectura">
            <p>{{ m.descripcion }}</p>
            <div class="dato-fijo"><span class="ms">person</span><span>Responsable: <strong>{{ m.responsableNombre }}</strong> · {{ fCorta(m.fechaCompromiso) }}</span></div>
            @if (m.respuesta) { <div class="dato-fijo"><span class="ms">reply</span><span>{{ m.respuesta }}</span></div> }
            <p class="meta">La editan el juez de paz y {{ m.responsableNombre }}.</p>
          </div>
        }
      }
    </ng-template>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .filtros select {
      width: 100%; margin-bottom: 10px; padding: 10px 12px; border-radius: 12px; border: 1px solid rgba(18,52,59,.16);
      background: var(--papel); color: var(--petroleo); font-size: 14px;
    }
    .kanban { display: flex; gap: 10px; overflow-x: auto; margin: 0 -18px; padding: 0 18px 6px; scroll-snap-type: x mandatory; }
    .columna { flex: none; width: 250px; display: flex; flex-direction: column; gap: 8px; padding: 10px; border-radius: 18px;
      background: rgba(18,52,59,.05); scroll-snap-align: start; }
    .col-cab { display: flex; align-items: center; justify-content: space-between; }
    .col-cab .n { font-family: var(--serif); font-size: 20px; color: var(--petroleo); }
    .mejora { border: none; text-align: left; cursor: pointer; padding: 12px; display: flex; flex-direction: column; gap: 6px; }
    .mejora.vencida { box-shadow: inset 3px 0 0 var(--rechazo), var(--anillo); }
    .fila { display: flex; align-items: center; gap: 4px; }
    .origen { font-size: 17px; color: var(--gris-claro); }
    .desc { font-size: 13px; line-height: 1.4; color: var(--texto); display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
    .meta { font-size: 12px; color: var(--gris); }
    .rojo { color: var(--rechazo-txt); font-weight: 600; }
    .vacia { padding: 14px 6px; text-align: center; font-family: var(--serif); font-style: italic; font-size: 14px; color: var(--gris); }
    .dos { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
    .opciones { display: flex; flex-wrap: wrap; gap: 6px; }
    .solo-lectura { display: flex; flex-direction: column; gap: 10px; margin-top: 14px; font-size: 14px; line-height: 1.5; color: var(--texto-2); }
    .solo-lectura p { margin: 0; }
  `,
})
export class TableroComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  readonly ui = inject(UiService);

  /** ?mejora=ID abre esa mejora (link desde Objeciones). */
  readonly mejora = input<string>();

  readonly fCorta = fCorta;
  readonly columnas = ESTADOS_MEJORA_LISTA;
  readonly est = ESTADOS_MEJORA;
  readonly origenes = ORIGENES_MEJORA_LISTA;
  readonly orig = ORIGENES_MEJORA;
  readonly vacias: Record<EstadoMejora, string> = {
    NUEVA: 'Nada nuevo bajo el sol.',
    EN_ANALISIS: 'Nadie está pensando. Por ahora.',
    EN_CURSO: 'Las manos, quietas.',
    RESUELTA: 'Todavía nada resuelto. Ánimo.',
    DESCARTADA: 'No se descartó nada: todo vale.',
  };

  readonly mejoras = signal<Mejora[]>([]);
  readonly usuarios = signal<Usuario[]>([]);
  readonly filtroOrigen = signal<OrigenMejora | null>(null);
  readonly filtroProc = signal<string | null>(null);
  readonly sel = signal<Mejora | null>(null);
  readonly ocupado = signal(false);
  readonly error = signal<string | null>(null);
  readonly esJuez = computed(() => this.auth.usuario()?.rol === 'JUEZ_PAZ');
  private readonly hoja = viewChild.required<TemplateRef<unknown>>('hojaMejora');
  fm = { descripcion: '', responsableId: 0, fechaCompromiso: '', estado: 'NUEVA' as EstadoMejora, respuesta: '' };

  readonly procedimientos = computed(() => [...new Set(this.mejoras().map((m) => m.procedimientoCodigo))].sort());
  readonly porEstado = computed(() => {
    const o = this.filtroOrigen();
    const p = this.filtroProc();
    const r = Object.fromEntries(this.columnas.map((c) => [c, [] as Mejora[]])) as Record<EstadoMejora, Mejora[]>;
    for (const m of this.mejoras()) {
      if ((o && m.origen !== o) || (p && m.procedimientoCodigo !== p)) continue;
      r[m.estado].push(m);
    }
    return r;
  });

  ngOnInit(): void {
    this.cargar(true);
    if (this.esJuez()) this.api.usuarios().subscribe((u) => this.usuarios.set(u.filter((x) => x.rol !== 'DIRECCION')));
  }

  private cargar(abrirDeLink = false): void {
    this.api.mejoras().subscribe({
      next: (ms) => {
        this.mejoras.set(ms);
        const id = Number(this.mejora());
        const m = abrirDeLink && id ? ms.find((x) => x.id === id) : undefined;
        if (m) this.abrir(m);
      },
      error: (e) => this.ui.error(e),
    });
  }

  puedeEditar(m: Mejora): boolean {
    return this.esJuez() || m.responsableId === this.auth.usuario()?.id;
  }

  cierra(): boolean {
    return this.fm.estado === 'RESUELTA' || this.fm.estado === 'DESCARTADA';
  }

  abrir(m: Mejora): void {
    this.sel.set(m);
    this.error.set(null);
    this.fm = {
      descripcion: m.descripcion,
      responsableId: m.responsableId,
      fechaCompromiso: m.fechaCompromiso,
      estado: m.estado,
      respuesta: m.respuesta ?? '',
    };
    this.ui.abrirHoja(this.hoja());
  }

  guardar(): void {
    const m = this.sel();
    if (!m) return;
    if (this.cierra() && !this.fm.respuesta.trim()) {
      this.error.set('Para cerrarla, registrá qué se hizo o por qué no se hace.');
      return;
    }
    this.ocupado.set(true);
    this.api
      .actualizarMejora(m.id, { ...this.fm, descripcion: this.fm.descripcion.trim(), respuesta: this.fm.respuesta.trim() || null })
      .subscribe({
        next: (r) => {
          this.ocupado.set(false);
          this.ui.cerrarHoja();
          this.mejoras.update((l) => l.map((x) => (x.id === r.id ? r : x)));
          this.ui.ok(r.estado === 'RESUELTA' ? `${r.codigo} resuelta.` : `${r.codigo} actualizada`);
        },
        error: (e) => {
          this.ocupado.set(false);
          this.error.set(mensajeDeError(e));
        },
      });
  }
}
