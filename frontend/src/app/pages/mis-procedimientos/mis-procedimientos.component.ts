import { Component, OnInit, TemplateRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { ESTADOS } from '../../core/diseno';
import { Procedimiento } from '../../core/models';
import { UiService, mensajeDeError } from '../../core/ui.service';

/** Procedimientos de los que soy dueño/a. Se editan tras una segunda cita que decidió ajustarlos (versión + 1). */
@Component({
  selector: 'app-mis-procedimientos',
  imports: [FormsModule],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pagina">Mis procedimientos</h1>
      <p class="subtitulo">Sos dueño/a del contenido: respondés las mejoras, firmás el acta si muere y lo ajustás después de una segunda cita.</p>
      <div class="lista">
        @for (p of lista(); track p.id) {
          <div class="tarjeta proc">
            <div class="fila">
              <span class="codigo">{{ p.codigo }} · v{{ p.version }}</span>
              <span class="flex"></span>
              <span class="chip" [style.background]="estados[p.estado][1]" [style.color]="estados[p.estado][2]">{{ estados[p.estado][0] }}</span>
            </div>
            <div class="nombre-serif">{{ p.nombre }}</div>
            @if (p.requiereReformulacion) {
              <div class="aviso"><span class="ms">restart_alt</span>Requiere reformulación: {{ p.segundasCitasConsecutivas }} segundas citas seguidas.</div>
            }
            @if (p.editablePorDueno) {
              <button class="btn-chico btn-ambar" (click)="editar(p, hoja)"><span class="ms">edit</span>Ajustar contenido</button>
            } @else {
              <div class="meta">Se edita después de una segunda cita que decida "ajustar y revotar".</div>
            }
          </div>
        } @empty {
          <div class="vacio-italico">No sos dueño/a de ningún procedimiento. Libre como el viento.</div>
        }
      </div>
    </div>

    <ng-template #hoja>
      <div class="titulo-hoja">Ajustar {{ sel()?.codigo }}</div>
      <p class="meta" style="margin: 4px 0 0">Guardar cambios sube la versión (v{{ sel()?.version }} → v{{ (sel()?.version ?? 0) + 1 }}).</p>
      <div class="form">
        <label class="campo"><span>Nombre</span><input [(ngModel)]="f.nombre" /></label>
        <label class="campo"><span>Bio (en primera persona)</span><textarea rows="3" class="poetico" [(ngModel)]="f.bio"></textarea></label>
        <div class="dos">
          <label class="campo"><span>Min. por uso</span><input type="number" min="0" [(ngModel)]="f.minutosPorUso" /></label>
          <label class="campo"><span>Usos por semana</span><input type="number" min="0" [(ngModel)]="f.usosPorSemana" /></label>
        </div>
        <label class="campo"><span>Te doy (beneficio)</span><input [(ngModel)]="f.beneficio" /></label>
        <label class="campo"><span>Versión express</span><textarea rows="3" [(ngModel)]="f.versionExpress"></textarea></label>
        @if (error()) { <div class="error-form">{{ error() }}</div> }
        <button class="btn btn-petroleo" (click)="guardar()" [disabled]="ocupado()">{{ ocupado() ? 'Guardando…' : 'Guardar nueva versión' }}</button>
      </div>
    </ng-template>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .proc { padding: 14px 16px; display: flex; flex-direction: column; gap: 8px; }
    .fila { display: flex; align-items: center; gap: 8px; }
    .meta { font-size: 12.5px; color: var(--gris); }
    .aviso { display: flex; align-items: center; gap: 6px; font-size: 12.5px; color: var(--rechazo-txt); font-weight: 600; }
    .aviso .ms { font-size: 17px; }
    .dos { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
  `,
})
export class MisProcedimientosComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly ui = inject(UiService);
  readonly estados = ESTADOS;
  readonly lista = signal<Procedimiento[]>([]);
  readonly sel = signal<Procedimiento | null>(null);
  readonly ocupado = signal(false);
  readonly error = signal<string | null>(null);
  f = { nombre: '', bio: '', minutosPorUso: 0, usosPorSemana: 0, beneficio: '', versionExpress: '' };

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.api.misProcedimientos().subscribe({ next: (l) => this.lista.set(l), error: (e) => this.ui.error(e) });
  }

  editar(p: Procedimiento, hoja: TemplateRef<unknown>): void {
    this.sel.set(p);
    this.error.set(null);
    this.f = {
      nombre: p.nombre,
      bio: p.bio,
      minutosPorUso: p.minutosPorUso,
      usosPorSemana: p.usosPorSemana,
      beneficio: p.beneficio,
      versionExpress: p.versionExpress ?? '',
    };
    this.ui.abrirHoja(hoja);
  }

  guardar(): void {
    const p = this.sel();
    if (!p) return;
    this.ocupado.set(true);
    this.api
      .editarContenido(p.id, {
        ...this.f,
        minutosPorUso: Math.max(0, Math.round(Number(this.f.minutosPorUso) || 0)),
        usosPorSemana: Math.max(0, Math.round(Number(this.f.usosPorSemana) || 0)),
        versionExpress: this.f.versionExpress.trim() || null,
      })
      .subscribe({
        next: (r) => {
          this.ocupado.set(false);
          this.ui.cerrarHoja();
          this.ui.ok(r.version > p.version ? `${r.codigo} ahora va por la versión ${r.version}` : 'Sin cambios: misma versión');
          this.cargar();
        },
        error: (e) => {
          this.ocupado.set(false);
          this.error.set(mensajeDeError(e));
        },
      });
  }
}
