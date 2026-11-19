import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Area, Celda, Matriz } from '../../core/models';
import { UiService } from '../../core/ui.service';

const clave = (u: number, p: number) => `${u}:${p}`;

/** Matriz de participación: usuario × procedimiento, con guardado por lote. */
@Component({
  selector: 'app-matriz',
  imports: [RouterLink],
  template: `
    <div class="pantalla">
      <a class="volver" routerLink="/juez-de-paz"><span class="ms">arrow_back</span>Juez de paz</a>
      <h1 class="titulo-pagina">Matriz de participación</h1>
      <p class="subtitulo">Marcá quién usa o ejecuta cada procedimiento: cada uno vota solo lo suyo.</p>

      <div class="pastillas">
        <button class="pastilla" [class.on]="areaId() === null" (click)="elegirArea(null)">Todas las áreas</button>
        @for (a of areas(); track a.id) {
          <button class="pastilla" [class.on]="areaId() === a.id" (click)="elegirArea(a.id)">{{ a.nombre }}</button>
        }
      </div>

      @if (matriz(); as m) {
        <div class="grilla-scroll">
          <table class="grilla">
            <thead>
              <tr>
                <th class="esquina">Persona</th>
                @for (p of m.procedimientos; track p.id) {
                  <th [title]="p.nombre"><span class="cod">{{ p.codigo }}</span></th>
                }
              </tr>
            </thead>
            <tbody>
              @for (u of m.usuarios; track u.id) {
                <tr>
                  <th class="persona">
                    <span class="nom">{{ u.nombre }}</span>
                    <span class="area">{{ u.areaNombre }}{{ u.rol === 'JUEZ_PAZ' ? ' · juez' : '' }}</span>
                  </th>
                  @for (p of m.procedimientos; track p.id) {
                    <td [class.cambio]="cambiado(u.id, p.id)">
                      <input type="checkbox" [checked]="marcado(u.id, p.id)" (change)="alternar(u.id, p.id)"
                             [attr.aria-label]="u.nombre + ' usa ' + p.codigo" />
                    </td>
                  }
                </tr>
              }
            </tbody>
          </table>
        </div>
        <div class="totales">
          @for (p of m.procedimientos; track p.id) {
            <span><strong>{{ p.codigo }}</strong> {{ habilitados(p.id) }}</span>
          }
        </div>
      } @else {
        <div class="vacio-italico">Armando la grilla…</div>
      }
    </div>

    @if (cambios() > 0) {
      <div class="barra-guardar">
        <span class="flex">{{ cambios() }} {{ cambios() === 1 ? 'cambio' : 'cambios' }} sin guardar</span>
        <button class="btn-texto" (click)="descartar()">Descartar</button>
        <button class="btn btn-ambar" (click)="guardar()" [disabled]="ocupado()">{{ ocupado() ? 'Guardando…' : 'Guardar lote' }}</button>
      </div>
    } @else if (matriz()) {
      <div class="barra-guardar tranquila">
        <span class="flex">Sin cambios. ¿Está todo bien? Igual podés dejar constancia.</span>
        <button class="btn btn-petroleo" (click)="guardar()" [disabled]="ocupado()">Marcar revisada</button>
      </div>
    }
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .volver { display: inline-flex; align-items: center; gap: 4px; margin-bottom: 12px; font-size: 13px; font-weight: 600; color: var(--petroleo); text-decoration: none; }
    .volver .ms { font-size: 18px; }
    .grilla-scroll { overflow: auto; max-height: 52vh; border-radius: 16px; background: var(--papel); box-shadow: var(--anillo); }
    .grilla { border-collapse: separate; border-spacing: 0; font-size: 12.5px; }
    .grilla th, .grilla td { border-bottom: 1px solid rgba(18,52,59,.07); }
    thead th { position: sticky; top: 0; z-index: 2; background: var(--papel); padding: 10px 6px; }
    .esquina { left: 0; z-index: 3; text-align: left; font-size: 11px; color: var(--gris); text-transform: uppercase; letter-spacing: .06em; }
    .cod { font-family: var(--mono); font-size: 11px; color: var(--petroleo); white-space: nowrap; }
    .persona { position: sticky; left: 0; z-index: 1; background: var(--papel); text-align: left; padding: 8px 10px; min-width: 132px;
      box-shadow: 1px 0 0 rgba(18,52,59,.08); }
    .nom { display: block; font-weight: 600; color: var(--petroleo); white-space: nowrap; }
    .area { display: block; font-size: 10.5px; font-weight: 400; color: var(--gris); white-space: nowrap; }
    td { text-align: center; padding: 6px 8px; }
    td.cambio { background: #FBE9CC; }
    input[type=checkbox] { width: 18px; height: 18px; accent-color: var(--match); cursor: pointer; }
    .totales { display: flex; flex-wrap: wrap; gap: 4px 12px; margin-top: 10px; font-size: 11.5px; color: var(--gris); }
    .totales strong { font-family: var(--mono); font-weight: 500; color: var(--petroleo); }
    .barra-guardar { flex: none; display: flex; align-items: center; gap: 8px; padding: 10px 14px; background: var(--petroleo); color: var(--crema); font-size: 13px; }
    .barra-guardar .btn { height: 40px; padding: 0 14px; border-radius: 12px; font-size: 13.5px; }
    .barra-guardar .btn-texto { color: rgba(245,240,230,.75); height: 40px; }
    .barra-guardar.tranquila { background: rgba(18,52,59,.06); color: var(--texto-2); }
  `,
})
export class MatrizComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly ui = inject(UiService);

  readonly areas = signal<Area[]>([]);
  readonly areaId = signal<number | null>(null);
  readonly matriz = signal<Matriz | null>(null);
  /** Estado original y estado editado, como conjuntos de "usuario:procedimiento". */
  private readonly original = signal<Set<string>>(new Set());
  private readonly actual = signal<Set<string>>(new Set());
  readonly ocupado = signal(false);

  readonly cambios = computed(() => {
    const a = this.actual();
    const o = this.original();
    let n = 0;
    a.forEach((k) => !o.has(k) && n++);
    o.forEach((k) => !a.has(k) && n++);
    return n;
  });

  ngOnInit(): void {
    this.api.areas().subscribe((a) => this.areas.set(a));
    this.cargar();
  }

  elegirArea(id: number | null): void {
    if (this.cambios() > 0) {
      this.ui.alerta('Guardá o descartá los cambios antes de cambiar de área.');
      return;
    }
    this.areaId.set(id);
    this.cargar();
  }

  private cargar(): void {
    this.matriz.set(null);
    this.api.matriz(this.areaId()).subscribe({
      next: (m) => {
        const set = new Set(m.celdas.map((c) => clave(c.usuarioId, c.procedimientoId)));
        this.original.set(set);
        this.actual.set(new Set(set));
        this.matriz.set(m);
      },
      error: (e) => this.ui.error(e),
    });
  }

  marcado(u: number, p: number): boolean {
    return this.actual().has(clave(u, p));
  }

  cambiado(u: number, p: number): boolean {
    const k = clave(u, p);
    return this.actual().has(k) !== this.original().has(k);
  }

  habilitados(p: number): number {
    let n = 0;
    this.actual().forEach((k) => k.endsWith(`:${p}`) && n++);
    return n;
  }

  alternar(u: number, p: number): void {
    const k = clave(u, p);
    const s = new Set(this.actual());
    if (s.has(k)) s.delete(k);
    else s.add(k);
    this.actual.set(s);
  }

  descartar(): void {
    this.actual.set(new Set(this.original()));
  }

  guardar(): void {
    const aCelda = (k: string): Celda => {
      const [u, p] = k.split(':').map(Number);
      return { usuarioId: u, procedimientoId: p };
    };
    const altas = [...this.actual()].filter((k) => !this.original().has(k)).map(aCelda);
    const bajas = [...this.original()].filter((k) => !this.actual().has(k)).map(aCelda);
    this.ocupado.set(true);
    this.api.guardarMatriz(altas, bajas).subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.ui.ok(r.mensaje);
        this.original.set(new Set(this.actual()));
      },
      error: (e) => {
        this.ocupado.set(false);
        this.ui.error(e);
      },
    });
  }
}
