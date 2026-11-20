import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { fCorta } from '../../core/diseno';
import { Area, Calendario, Designacion, Usuario } from '../../core/models';
import { UiService } from '../../core/ui.service';

/** JUEZ_PAZ: checklist del trimestre y accesos. DIRECCION: designación del juez con historial. */
@Component({
  selector: 'app-juez-de-paz',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pagina">Juez de paz</h1>
      @if (esJuez()) {
        <p class="subtitulo">Coordinás todo: matriz, ciclos, objeciones, citas, ceremonias y tablero.</p>

        <div class="accesos">
          <a class="acceso" routerLink="/matriz"><span class="ms">grid_on</span><strong>Matriz</strong><small>quién vota qué</small></a>
          <a class="acceso" routerLink="/objeciones">
            <span class="ms">record_voice_over</span><strong>Objeciones</strong>
            <small>{{ pendientes() === null ? '…' : pendientes() + ' sin responder' }}</small>
          </a>
          <a class="acceso" routerLink="/segundas-citas">
            <span class="ms">coffee</span><strong>Segundas citas</strong>
            <small>{{ citas() === null ? '…' : citas() + ' convocadas' }}</small>
          </a>
          <a class="acceso" routerLink="/gestion"><span class="ms">tune</span><strong>Gestión</strong><small>procedimientos y ciclos</small></a>
        </div>

        <div class="rotulo" style="margin-top: 20px">Calendario del trimestre {{ calendario()?.ciclo?.nombre ?? '' }}</div>
        <div class="tarjeta checklist">
          @for (i of calendario()?.items ?? []; track i.titulo) {
            <div class="item" [class.hecho]="i.hecho">
              <span class="ms tilde">{{ i.hecho ? 'check_circle' : 'radio_button_unchecked' }}</span>
              <div class="flex">
                <div class="semana">{{ i.semana }}</div>
                <div class="titulo">{{ i.titulo }}</div>
                <div class="detalle">{{ i.detalle }}</div>
              </div>
            </div>
          } @empty {
            <div class="item"><div class="detalle">Cargando el calendario…</div></div>
          }
        </div>
        @if (hechos() !== null) {
          <p class="pie">{{ hechos() === total() ? 'Todas las tareas del trimestre están completas.' : hechos() + ' de ' + total() + ' tareas hechas.' }}</p>
        }
      } @else {
        <p class="subtitulo">La Dirección es padrino institucional: designa al juez de paz de cada área (rotación anual) y firma las actas.</p>

        <div class="tarjeta designar">
          <div class="rotulo" style="margin: 0">Nueva designación</div>
          <label class="campo">
            <span>Área</span>
            <select [(ngModel)]="areaId" (ngModelChange)="elegirArea($event)">
              <option [ngValue]="null" disabled>Elegí un área…</option>
              @for (a of areas(); track a.id) { <option [ngValue]="a.id">{{ a.nombre }}</option> }
            </select>
          </label>
          @if (areaId) {
            <div class="dato-fijo">
              <span class="ms">gavel</span>
              <span>@if (vigente(); as v) { Vigente: <strong>{{ v.usuarioNombre }}</strong> desde el {{ fCorta(v.desde) }} } @else { Sin juez vigente en esta área }</span>
            </div>
            <label class="campo">
              <span>Nuevo juez de paz</span>
              <select [(ngModel)]="usuarioId">
                <option [ngValue]="null" disabled>Elegí a alguien del área…</option>
                @for (u of candidatos(); track u.id) { <option [ngValue]="u.id">{{ u.nombre }}</option> }
              </select>
            </label>
            <p class="nota">Al designar, la designación vigente se cierra hoy y el juez anterior vuelve a ser empleado/a.</p>
            <button class="btn btn-petroleo" (click)="designar()" [disabled]="ocupado() || !usuarioId">{{ ocupado() ? 'Designando…' : 'Designar' }}</button>
          }
        </div>

        <div class="rotulo" style="margin-top: 20px">Historial de designaciones</div>
        <div class="tarjeta historial">
          @for (d of designaciones(); track d.id) {
            <div class="renglon">
              <span class="ms" [style.color]="d.vigente ? '#2E7D73' : '#8A9699'">{{ d.vigente ? 'gavel' : 'history' }}</span>
              <div class="flex">
                <div class="titulo">{{ d.usuarioNombre }}</div>
                <div class="detalle">{{ d.areaNombre }} · {{ fCorta(d.desde) }} → {{ d.hasta ? fCorta(d.hasta) : 'hoy' }}</div>
              </div>
              @if (d.vigente) { <span class="chip" style="background: #DCEDEA; color: #1D5A52">Vigente</span> }
            </div>
          } @empty {
            <div class="renglon"><div class="detalle">Todavía no se designó a nadie.</div></div>
          }
        </div>
      }
    </div>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .accesos { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
    .acceso { display: flex; flex-direction: column; gap: 4px; padding: 14px; border-radius: 18px; background: var(--papel);
      box-shadow: var(--anillo); text-decoration: none; color: var(--petroleo); }
    .acceso .ms { font-size: 24px; color: var(--ambar); }
    .acceso strong { font-family: var(--serif); font-weight: 500; font-size: 18px; }
    .acceso small { font-size: 12px; color: var(--gris); }
    .checklist, .historial { overflow: hidden; }
    .item, .renglon { display: flex; align-items: flex-start; gap: 12px; padding: 12px 14px; }
    .item + .item, .renglon + .renglon { border-top: 1px solid rgba(18,52,59,.07); }
    .tilde { font-size: 22px; color: var(--gris-claro); }
    .item.hecho .tilde { color: var(--match); font-variation-settings: 'FILL' 1; }
    .semana { font-size: 10.5px; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; color: #9A5B12; }
    .titulo { font-weight: 600; font-size: 14px; color: var(--petroleo); }
    .detalle { font-size: 12.5px; line-height: 1.4; color: var(--gris); margin-top: 2px; }
    .pie { margin: 10px 0 0; text-align: center; font-family: var(--serif); font-style: italic; font-size: 16px; color: var(--gris); }
    .designar { padding: 16px; display: flex; flex-direction: column; gap: 12px; }
    .nota { margin: 0; font-size: 12.5px; color: var(--gris); }
    .renglon .ms { font-size: 20px; }
  `,
})
export class JuezDePazComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly ui = inject(UiService);
  readonly fCorta = fCorta;

  readonly esJuez = computed(() => this.auth.usuario()?.rol === 'JUEZ_PAZ');
  readonly calendario = signal<Calendario | null>(null);
  readonly pendientes = signal<number | null>(null);
  readonly citas = signal<number | null>(null);
  readonly hechos = computed(() => {
    const c = this.calendario();
    return c ? c.items.filter((i) => i.hecho).length : null;
  });
  readonly total = computed(() => this.calendario()?.items.length ?? 0);

  readonly areas = signal<Area[]>([]);
  readonly usuarios = signal<Usuario[]>([]);
  readonly designaciones = signal<Designacion[]>([]);
  readonly ocupado = signal(false);
  private readonly areaSel = signal<number | null>(null);
  areaId: number | null = null;
  usuarioId: number | null = null;
  readonly vigente = computed(() => this.designaciones().find((d) => d.vigente && d.areaId === this.areaSel()) ?? null);
  readonly candidatos = computed(() =>
    this.usuarios().filter((u) => u.areaId === this.areaSel() && u.rol !== 'DIRECCION' && u.id !== this.vigente()?.usuarioId),
  );

  ngOnInit(): void {
    if (this.esJuez()) {
      this.api.calendario().subscribe({ next: (c) => this.calendario.set(c), error: (e) => this.ui.error(e) });
      this.api.objeciones().subscribe({ next: (g) => this.pendientes.set(g.reduce((s, x) => s + x.pendientes, 0)) });
      this.api.segundasCitas().subscribe({ next: (c) => this.citas.set(c.length) });
    } else {
      this.api.areas().subscribe((a) => this.areas.set(a));
      this.api.usuarios().subscribe((u) => this.usuarios.set(u));
      this.cargarHistorial();
    }
  }

  private cargarHistorial(): void {
    this.api.designaciones().subscribe({ next: (d) => this.designaciones.set(d), error: (e) => this.ui.error(e) });
  }

  elegirArea(id: number): void {
    this.areaSel.set(id);
    this.usuarioId = null;
  }

  designar(): void {
    if (!this.areaId || !this.usuarioId) return;
    this.ocupado.set(true);
    this.api.designar(this.usuarioId, this.areaId).subscribe({
      next: (d) => {
        this.ocupado.set(false);
        this.usuarioId = null;
        this.ui.ok(`${d.usuarioNombre} es juez de paz de ${d.areaNombre}.`);
        this.cargarHistorial();
        this.api.usuarios().subscribe((u) => this.usuarios.set(u));
      },
      error: (e) => {
        this.ocupado.set(false);
        this.ui.error(e);
      },
    });
  }
}
