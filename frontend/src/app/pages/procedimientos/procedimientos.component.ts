import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { ApiService } from '../../core/api.service';
import { ESTADOS, ESTADOS_LISTA } from '../../core/etiquetas';
import { EstadoProcedimiento, Procedimiento } from '../../core/models';
import { NotificadorService } from '../../core/notificador.service';
import { EstadoChipComponent } from '../../shared/estado-chip.component';
import { ProcedimientoDialogComponent } from './procedimiento-dialog.component';

@Component({
  selector: 'app-procedimientos',
  imports: [MatButtonModule, EstadoChipComponent],
  template: `
    <section class="pagina">
      <header class="fila">
        <div>
          <h1>📄 Procedimientos</h1>
          <p>ABM de Calidad. Los que ya tienen historia no se borran: se velan.</p>
        </div>
        <span class="espaciador"></span>
        <button mat-flat-button (click)="abrir()">+ Nuevo procedimiento</button>
      </header>

      <div class="filtros">
        <button class="filtro" [class.activo]="!estado()" (click)="filtrar(null)">Todos</button>
        @for (e of estados; track e) {
          <button class="filtro" [class.activo]="estado() === e" (click)="filtrar(e)">{{ etiquetas[e].icono }} {{ etiquetas[e].texto }}</button>
        }
      </div>

      <div class="grilla">
        @for (p of lista(); track p.id) {
          <article class="tarjeta-blanca item" [class.velado]="p.estado === 'VELADO'">
            <div class="fila sup">
              <strong class="codigo">{{ p.codigo }}</strong>
              <app-estado-chip [estado]="p.estado" />
            </div>
            <h3>{{ p.nombre }}</h3>
            <p class="bio">“{{ p.bio }}”</p>
            <p class="meta">
              {{ p.tipo === 'REGISTRO' ? 'Registro' : 'Procedimiento' }} · {{ p.edadAnios }} años ·
              {{ p.minutosSemanales }} min/semana · padrino: {{ p.padrinoNombre ?? 'nadie' }}
            </p>
            <p class="meta">📍 {{ nombresAreas(p) }}</p>
            @if (p.obligatorio) { <p class="meta oblig">⚖️ Obligatorio: {{ p.baseObligatoriedad }}</p> }
            <div class="acciones">
              <button mat-button (click)="abrir(p)" [disabled]="p.estado === 'VELADO'">Editar</button>
              <button mat-button class="borrar" (click)="borrar(p)">Borrar</button>
            </div>
          </article>
        } @empty {
          <div class="vacio tarjeta-blanca">No hay procedimientos en este estado.</div>
        }
      </div>
    </section>
  `,
  styles: `
    .filtros { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 16px; }
    .filtro {
      border: 1px solid #cfd8da; background: #fff; border-radius: 999px; padding: 6px 12px;
      cursor: pointer; font: inherit; font-size: .85rem; color: var(--petroleo);
    }
    .filtro.activo { background: var(--petroleo); color: #fff; border-color: var(--petroleo); }
    .grilla { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
    .item { margin: 0; display: flex; flex-direction: column; }
    .item.velado { opacity: .65; }
    .sup { justify-content: space-between; }
    .sup > * { flex: 0 0 auto; }
    .codigo { font-family: var(--fuente-titulos); color: var(--ambar); font-size: 1.1rem; }
    h3 { font-size: 1.1rem; margin: 6px 0; }
    .bio { font-style: italic; color: #4a5a5d; font-size: .9rem; margin: 0 0 8px; flex: 1; }
    .meta { font-size: .8rem; color: #6b7a7d; margin: 2px 0; }
    .oblig { color: #8a5a00; }
    .acciones { display: flex; justify-content: flex-end; margin-top: 8px; }
    .borrar { color: var(--rechazo); }
  `,
})
export class ProcedimientosComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly notificador = inject(NotificadorService);

  readonly estados = ESTADOS_LISTA;
  readonly etiquetas = ESTADOS;
  readonly lista = signal<Procedimiento[]>([]);
  readonly estado = signal<EstadoProcedimiento | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  filtrar(e: EstadoProcedimiento | null): void {
    this.estado.set(e);
    this.cargar();
  }

  cargar(): void {
    this.api.procedimientos(this.estado()).subscribe({
      next: (l) => this.lista.set(l),
      error: (e) => this.notificador.error(e),
    });
  }

  nombresAreas(p: Procedimiento): string {
    return p.areas.map((a) => a.nombre).join(' · ');
  }

  abrir(p?: Procedimiento): void {
    this.dialog
      .open(ProcedimientoDialogComponent, { data: p ?? null, width: '640px', maxWidth: '96vw', autoFocus: false })
      .afterClosed()
      .subscribe((guardado) => {
        if (guardado) {
          this.notificador.ok(p ? 'Perfil actualizado' : 'Nuevo procedimiento en el Tinder 🔥');
          this.cargar();
        }
      });
  }

  borrar(p: Procedimiento): void {
    if (!confirm(`¿Borrar ${p.codigo}? Solo se puede si nunca entró en juego.`)) return;
    this.api.eliminarProcedimiento(p.id).subscribe({
      next: () => {
        this.notificador.ok(`${p.codigo} borrado`);
        this.cargar();
      },
      error: (e) => this.notificador.error(e),
    });
  }
}
