import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { CATEGORIAS, ESTADOS } from '../../core/diseno';
import { GrupoObjeciones, Objecion, OrigenObjecion } from '../../core/models';
import { UiService, mensajeDeError } from '../../core/ui.service';

interface Borrador {
  fundada: boolean | null;
  respuesta: string;
  fecha: string;
  error: string | null;
  ocupado: boolean;
}

const ORIGENES: Record<OrigenObjecion, string> = { VOTO: 'Al votar', BODA: 'En la boda', ANIVERSARIO: 'En el aniversario' };

/** Objeciones agrupadas por procedimiento y categoría. Nunca se muestra autor: no existe. */
@Component({
  selector: 'app-objeciones',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="pantalla">
      <a class="volver" routerLink="/juez-de-paz"><span class="ms">arrow_back</span>Juez de paz</a>
      <h1 class="titulo-pagina">Objeciones</h1>
      <p class="subtitulo">La voz de la minoría, agrupada. Anónimas de verdad: no hay autor que mostrar.</p>

      <div class="segmentado">
        <button [class.on]="soloPendientes()" (click)="soloPendientes.set(true)">Pendientes ({{ pendientes() }})</button>
        <button [class.on]="!soloPendientes()" (click)="soloPendientes.set(false)">Todas</button>
      </div>

      <div class="lista" style="gap: 14px">
        @for (g of visibles(); track g.procedimientoId) {
          <div class="tarjeta grupo">
            <div class="cab">
              <div class="flex" style="min-width: 0">
                <div class="codigo">{{ g.codigo }}</div>
                <div class="nombre-serif">{{ g.nombre }}</div>
              </div>
              <span class="chip" [style.background]="estados[g.estadoProcedimiento][1]" [style.color]="estados[g.estadoProcedimiento][2]">
                {{ estados[g.estadoProcedimiento][0] }}
              </span>
            </div>
            <div class="resumen">{{ g.total }} {{ g.total === 1 ? 'objeción' : 'objeciones' }} · {{ g.pendientes }} sin evaluar</div>
            @for (c of g.categorias; track c.categoria) {
              <div class="categoria">
                <div class="cat-cab"><span class="cant">{{ c.cantidad }}</span>{{ cat[c.categoria] }}</div>
                @for (o of filtrar(c.objeciones); track o.id) {
                  <div class="objecion" [class.hecha]="o.estado !== 'PENDIENTE'">
                    <div class="texto">{{ o.texto ? '«' + o.texto + '»' : 'Sin comentario, solo la categoría.' }}</div>
                    <div class="meta">{{ origenes[o.origen] }}{{ o.cicloNombre ? ' · ' + o.cicloNombre : '' }}</div>
                    @if (o.estado === 'PENDIENTE' || editando() === o.id) {
                      <div class="toggle">
                        <button [class.on]="b(o).fundada === true" (click)="b(o).fundada = true"><span class="ms">thumb_up</span>Fundada</button>
                        <button [class.on]="b(o).fundada === false" (click)="b(o).fundada = false"><span class="ms">thumb_down</span>No fundada</button>
                      </div>
                      <textarea rows="2" [(ngModel)]="b(o).respuesta" placeholder="Respuesta (obligatoria): nadie queda sin respuesta"></textarea>
                      <label class="fecha">Mejora para el <input type="date" [(ngModel)]="b(o).fecha" /> <span class="meta">(por defecto, 30 días)</span></label>
                      @if (b(o).error) { <div class="error-form">{{ b(o).error }}</div> }
                      <button class="btn btn-petroleo guardar" (click)="evaluar(o)" [disabled]="b(o).ocupado">{{ b(o).ocupado ? 'Guardando…' : 'Responder' }}</button>
                    } @else {
                      <div class="evaluada">
                        <span class="chip" [style.background]="o.fundada ? '#DCEDEA' : '#ECE8E0'" [style.color]="o.fundada ? '#1D5A52' : '#4F5958'">
                          {{ o.fundada ? 'Fundada' : 'No fundada' }}
                        </span>
                        @if (o.mejoraId) {
                          <a class="link" [routerLink]="['/tablero']" [queryParams]="{ mejora: o.mejoraId }"><span class="ms">view_kanban</span>{{ o.mejoraCodigo }}</a>
                        }
                        <button class="btn-texto mini" (click)="editar(o)">Editar</button>
                      </div>
                      <div class="respuesta">{{ o.respuesta ?? 'Fundada, sin respuesta todavía (bloquea el casamiento).' }}</div>
                    }
                  </div>
                }
              </div>
            }
          </div>
        } @empty {
          <div class="vacio-italico">{{ soloPendientes() ? 'Nadie se queja. ¿Será amor o miedo?' : 'Todavía no hay objeciones.' }}</div>
        }
      </div>
    </div>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .volver { display: inline-flex; align-items: center; gap: 4px; margin-bottom: 12px; font-size: 13px; font-weight: 600; color: var(--petroleo); text-decoration: none; }
    .volver .ms { font-size: 18px; }
    .grupo { padding: 14px 16px; display: flex; flex-direction: column; gap: 10px; }
    .cab { display: flex; align-items: flex-start; gap: 10px; }
    .resumen { font-size: 12.5px; color: var(--gris); }
    .categoria { display: flex; flex-direction: column; gap: 8px; padding-top: 10px; border-top: 1px solid rgba(18,52,59,.07); }
    .cat-cab { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 13.5px; color: var(--petroleo); }
    .cant { min-width: 24px; height: 24px; padding: 0 6px; border-radius: 12px; background: var(--ambar); color: var(--ambar-tinta);
      display: grid; place-items: center; font-size: 12px; font-weight: 700; }
    .objecion { display: flex; flex-direction: column; gap: 6px; padding: 10px 12px; border-radius: 12px; background: rgba(18,52,59,.04); }
    .objecion.hecha { background: transparent; border: 1px dashed rgba(18,52,59,.14); }
    .texto { font-family: var(--serif); font-style: italic; font-size: 15px; line-height: 1.4; color: var(--texto); }
    .meta { font-size: 11.5px; color: var(--gris); }
    .toggle { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
    .toggle button { display: flex; align-items: center; justify-content: center; gap: 5px; height: 36px; border-radius: 10px;
      border: 1px solid rgba(18,52,59,.16); background: #fff; color: var(--gris); font-weight: 600; font-size: 13px; cursor: pointer; }
    .toggle button .ms { font-size: 17px; }
    .toggle button.on:first-child { background: var(--match); border-color: var(--match); color: #fff; }
    .toggle button.on:last-child { background: var(--petroleo); border-color: var(--petroleo); color: var(--crema); }
    textarea, input[type=date] { width: 100%; padding: 10px 12px; border-radius: 10px; border: 1px solid rgba(18,52,59,.16); background: #fff; font-size: 13.5px; }
    input[type=date] { width: auto; padding: 6px 8px; }
    .fecha { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; font-size: 12.5px; color: var(--texto-2); }
    .guardar { height: 40px; font-size: 14px; }
    .evaluada { display: flex; align-items: center; gap: 10px; }
    .link { display: inline-flex; align-items: center; gap: 4px; font-size: 12.5px; font-weight: 600; text-decoration: none; }
    .link .ms { font-size: 16px; }
    .mini { height: 28px; margin-left: auto; font-size: 12.5px; }
    .respuesta { font-size: 13px; line-height: 1.45; color: var(--texto-2); }
  `,
})
export class ObjecionesComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly ui = inject(UiService);
  readonly cat = CATEGORIAS;
  readonly estados = ESTADOS;
  readonly origenes = ORIGENES;

  readonly grupos = signal<GrupoObjeciones[]>([]);
  readonly soloPendientes = signal(true);
  readonly editando = signal<string | null>(null);
  private readonly borradores = new Map<string, Borrador>();

  readonly pendientes = computed(() => this.grupos().reduce((s, g) => s + g.pendientes, 0));
  readonly visibles = computed(() => (this.soloPendientes() ? this.grupos().filter((g) => g.pendientes > 0) : this.grupos()));

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.api.objeciones().subscribe({ next: (g) => this.grupos.set(g), error: (e) => this.ui.error(e) });
  }

  filtrar(lista: Objecion[]): Objecion[] {
    return this.soloPendientes() ? lista.filter((o) => o.estado === 'PENDIENTE' || this.editando() === o.id) : lista;
  }

  b(o: Objecion): Borrador {
    let x = this.borradores.get(o.id);
    if (!x) {
      x = { fundada: o.fundada, respuesta: o.respuesta ?? '', fecha: '', error: null, ocupado: false };
      this.borradores.set(o.id, x);
    }
    return x;
  }

  editar(o: Objecion): void {
    this.borradores.delete(o.id);
    this.editando.set(o.id);
  }

  evaluar(o: Objecion): void {
    const x = this.b(o);
    if (x.fundada === null) {
      x.error = 'Marcá si es fundada o no.';
      return;
    }
    if (!x.respuesta.trim()) {
      x.error = 'Escribí la respuesta: nadie queda sin respuesta.';
      return;
    }
    x.error = null;
    x.ocupado = true;
    this.api.evaluarObjecion(o.id, { fundada: x.fundada, respuesta: x.respuesta.trim(), fechaCompromiso: x.fecha || null }).subscribe({
      next: (r) => {
        x.ocupado = false;
        this.editando.set(null);
        this.ui.ok(r.mejoraCodigo ? `Respondida. Mejora ${r.mejoraCodigo} al tablero.` : 'Respondida.');
        this.ui.avisarCambio();
        this.cargar();
      },
      error: (e) => {
        x.ocupado = false;
        x.error = mensajeDeError(e);
      },
    });
  }
}
