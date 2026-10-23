import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Area, Ciclo, Resultado } from '../../core/models';
import { NotificadorService } from '../../core/notificador.service';
import { BarraVotosComponent } from '../../shared/barra-votos.component';
import { VeredictoChipComponent } from '../../shared/veredicto-chip.component';

interface Grupo {
  area: string;
  filas: Resultado[];
}

@Component({
  selector: 'app-resultados',
  imports: [FormsModule, MatFormFieldModule, MatSelectModule, BarraVotosComponent, VeredictoChipComponent],
  template: `
    <section class="pagina">
      <header>
        <h1>📊 Resultados por área</h1>
        <p>
          @if (esEmpleado()) { Ves los resultados agregados de tu área ({{ auth.usuario()?.areaNombre }}). }
          @else { Calidad y Dirección ven todas las áreas. }
          Nunca se muestra quién votó qué.
        </p>
      </header>

      <div class="tarjeta-blanca fila filtros">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Ciclo</mat-label>
          <mat-select [(ngModel)]="cicloId" (selectionChange)="cargar()">
            @for (c of ciclosCerrados(); track c.id) {
              <mat-option [value]="c.id">{{ c.nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        @if (!esEmpleado()) {
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>Área</mat-label>
            <mat-select [(ngModel)]="areaId" (selectionChange)="cargar()">
              <mat-option [value]="null">Todas</mat-option>
              @for (a of areas(); track a.id) {
                <mat-option [value]="a.id">{{ a.nombre }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        }
      </div>

      @if (grupos().length === 0 && !cargando()) {
        <div class="vacio tarjeta-blanca">
          <span class="emoji">🗳️</span>
          Todavía no hay resultados: se calculan cuando Calidad cierra el ciclo.
        </div>
      }

      @for (g of grupos(); track g.area) {
        <article class="tarjeta-blanca area">
          <h2>{{ g.area }}</h2>
          <div class="tabla">
            <div class="fila-res cabecera">
              <span>Procedimiento</span><span>Me sirve / me sobra</span><span>Veredicto</span>
            </div>
            @for (r of g.filas; track r.procedimientoId) {
              <div class="fila-res">
                <span class="nombre">
                  <strong>{{ r.codigo }}</strong> {{ r.nombre }}
                  @if (r.total !== null) { <small>{{ r.total }} votos</small> }
                </span>
                <app-barra-votos [pctLike]="r.pctLike" [pctNope]="r.pctNope" />
                <span class="veredicto">
                  <app-veredicto-chip [veredicto]="r.veredicto" />
                  @if (r.veredicto === 'QUORUM_INSUFICIENTE') {
                    <small class="nota">Pocos votos, por anonimato no mostramos números</small>
                  }
                </span>
              </div>
            }
          </div>
        </article>
      }
    </section>
  `,
  styles: `
    .filtros mat-form-field { max-width: 280px; }
    .area h2 { font-size: 1.2rem; border-bottom: 2px solid var(--ambar); padding-bottom: 6px; }
    .fila-res {
      display: grid; grid-template-columns: minmax(0, 1.3fr) minmax(0, 1.2fr) minmax(0, .8fr);
      gap: 14px; align-items: center; padding: 10px 0; border-bottom: 1px solid #eef0f0;
    }
    .fila-res:last-child { border-bottom: none; }
    .cabecera { font-size: .75rem; text-transform: uppercase; color: #8a9699; letter-spacing: .05em; padding-top: 0; }
    .nombre small { display: block; color: #8a9699; font-size: .75rem; }
    .veredicto { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; }
    .nota { font-size: .7rem; color: #6b7a7d; line-height: 1.2; }
    @media (max-width: 640px) {
      .fila-res { grid-template-columns: 1fr; gap: 6px; }
      .cabecera { display: none; }
    }
  `,
})
export class ResultadosComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notificador = inject(NotificadorService);
  readonly auth = inject(AuthService);

  readonly ciclosCerrados = signal<Ciclo[]>([]);
  readonly areas = signal<Area[]>([]);
  readonly resultados = signal<Resultado[]>([]);
  readonly cargando = signal(true);
  readonly esEmpleado = computed(() => this.auth.usuario()?.rol === 'EMPLEADO');
  cicloId: number | null = null;
  areaId: number | null = null;

  readonly grupos = computed<Grupo[]>(() => {
    const mapa = new Map<string, Resultado[]>();
    for (const r of this.resultados()) {
      mapa.set(r.areaNombre, [...(mapa.get(r.areaNombre) ?? []), r]);
    }
    return [...mapa.entries()].map(([area, filas]) => ({ area, filas }));
  });

  ngOnInit(): void {
    this.api.ciclos().subscribe((cs) => {
      const cerrados = cs.filter((c) => c.estado === 'CERRADO');
      this.ciclosCerrados.set(cerrados);
      this.cicloId = cerrados[0]?.id ?? null;
      this.cargar();
    });
    this.api.areas().subscribe((a) => this.areas.set(a));
  }

  cargar(): void {
    this.cargando.set(true);
    this.api.resultados(this.cicloId, this.areaId).subscribe({
      next: (r) => {
        this.resultados.set(r);
        this.cargando.set(false);
      },
      error: (e) => {
        this.notificador.error(e);
        this.cargando.set(false);
      },
    });
  }
}
