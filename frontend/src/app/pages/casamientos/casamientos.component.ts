import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { hoyIso } from '../../core/etiquetas';
import { Casamiento, Procedimiento, Usuario } from '../../core/models';
import { NotificadorService } from '../../core/notificador.service';

@Component({
  selector: 'app-casamientos',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <section class="pagina">
      <header>
        <h1>💍 Casamientos</h1>
        <p>Los procedimientos que hicieron match se casan en público: el equipo se compromete a usarlos.</p>
      </header>

      @if (esCalidad()) {
        <form class="tarjeta-blanca registro" [formGroup]="form" (ngSubmit)="casar()">
          <h2>Registrar casamiento</h2>
          @if (pendientes().length === 0) {
            <p class="vacio">No hay procedimientos pendientes de casamiento. Cerrá un ciclo para generar matches.</p>
          } @else {
            <div class="fila">
              <mat-form-field appearance="outline">
                <mat-label>Procedimiento con match</mat-label>
                <mat-select formControlName="procedimientoId" (selectionChange)="elegir($event.value)">
                  @for (p of pendientes(); track p.id) {
                    <mat-option [value]="p.id">{{ p.codigo }} {{ p.nombre }} ({{ p.areas.length }} áreas)</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Fecha</mat-label>
                <input matInput type="date" formControlName="fecha" />
              </mat-form-field>
            </div>
            <mat-form-field appearance="outline">
              <mat-label>Votos (lo que el equipo promete)</mat-label>
              <textarea matInput rows="3" formControlName="votos" placeholder="Prometemos usarte en las buenas y en las auditorías…"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Acuerdo prenupcial: versión express (una página para urgencias)</mat-label>
              <textarea matInput rows="3" formControlName="versionExpress"></textarea>
            </mat-form-field>
            <div class="fila">
              <mat-form-field appearance="outline">
                <mat-label>Oficiante (Dirección)</mat-label>
                <mat-select formControlName="oficianteId">
                  @for (o of oficiantes(); track o.id) { <mat-option [value]="o.id">{{ o.nombre }}</mat-option> }
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Padrino</mat-label>
                <mat-select formControlName="padrinoId">
                  <mat-option [value]="null">Sin padrino</mat-option>
                  @for (u of usuarios(); track u.id) { <mat-option [value]="u.id">{{ u.nombre }}</mat-option> }
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Áreas que se casan</mat-label>
                <mat-select formControlName="areaIds" multiple>
                  @for (a of elegido()?.areas ?? []; track a.id) { <mat-option [value]="a.id">{{ a.nombre }}</mat-option> }
                </mat-select>
              </mat-form-field>
            </div>
            @if (grupal()) {
              <p class="grupal">💍💍💍 Casamiento grupal: {{ cantidadAreas() }} áreas se comprometen juntas.</p>
            }
            <button mat-flat-button type="submit" [disabled]="form.invalid || guardando()">Sí, acepto 💍</button>
          }
        </form>
      }

      <div class="grilla">
        @for (c of casamientos(); track c.id) {
          <article class="tarjeta-blanca boda" [class]="'tarjeta-blanca boda ' + c.estado.toLowerCase()">
            <div class="fila sup">
              <strong class="codigo">{{ c.codigo }}</strong>
              <span class="chip" [class.veredicto-match]="c.estado === 'VIGENTE'" [class.veredicto-segunda]="c.estado === 'RENOVADO'" [class.veredicto-rechazo]="c.estado === 'DIVORCIADO'">{{ c.estado }}</span>
            </div>
            <h3>{{ c.nombre }} @if (c.grupal) { <span title="Casamiento grupal">💍💍</span> }</h3>
            <p class="meta">Con {{ c.areas.join(', ') }}</p>
            <p class="meta">{{ c.fecha | date: 'd/M/yyyy' }} · aniversario {{ c.fechaAniversario | date: 'd/M/yyyy' }}</p>
            <a mat-button [routerLink]="['/casamientos', c.id]">Ver libreta →</a>
          </article>
        } @empty {
          <div class="vacio tarjeta-blanca">Todavía no hubo casamientos.</div>
        }
      </div>
    </section>
  `,
  styles: `
    .registro { border-top: 6px solid var(--match); }
    .grupal { background: #e3f1ef; color: var(--match); border-radius: 10px; padding: 8px 12px; font-weight: 700; }
    .grilla { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
    .boda { margin: 0; border-left: 6px solid var(--match); }
    .boda.renovado { border-left-color: var(--ambar); }
    .boda.divorciado { border-left-color: var(--rechazo); opacity: .75; }
    .sup { justify-content: space-between; }
    .sup > * { flex: 0 0 auto; }
    .codigo { font-family: var(--fuente-titulos); color: var(--ambar); }
    h3 { margin: 6px 0; font-size: 1.1rem; }
    .meta { font-size: .85rem; color: #5b6b6e; margin: 2px 0; }
  `,
})
export class CasamientosComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly notificador = inject(NotificadorService);

  readonly esCalidad = computed(() => this.auth.tieneRol('CALIDAD'));
  readonly casamientos = signal<Casamiento[]>([]);
  readonly pendientes = signal<Procedimiento[]>([]);
  readonly oficiantes = signal<Usuario[]>([]);
  readonly usuarios = signal<Usuario[]>([]);
  readonly elegido = signal<Procedimiento | null>(null);
  readonly guardando = signal(false);
  readonly cantidadAreas = signal(0);
  readonly grupal = computed(() => this.cantidadAreas() >= 2);

  readonly form = inject(FormBuilder).group({
    procedimientoId: [null as number | null, Validators.required],
    fecha: [hoyIso()],
    votos: ['', Validators.required],
    versionExpress: ['', Validators.required],
    oficianteId: [null as number | null, Validators.required],
    padrinoId: [null as number | null],
    areaIds: [[] as number[], Validators.required],
  });

  ngOnInit(): void {
    this.form.controls.areaIds.valueChanges.subscribe((v) => this.cantidadAreas.set(v?.length ?? 0));
    this.cargar();
    if (this.esCalidad()) {
      this.api.usuarios('DIRECCION').subscribe((o) => {
        this.oficiantes.set(o);
        if (o.length) this.form.patchValue({ oficianteId: o[0].id });
      });
      this.api.usuarios().subscribe((u) => this.usuarios.set(u));
    }
  }

  cargar(): void {
    this.api.casamientos().subscribe({ next: (c) => this.casamientos.set(c), error: (e) => this.notificador.error(e) });
    if (this.esCalidad()) {
      this.api.procedimientos('PENDIENTE_CASAMIENTO').subscribe((p) => this.pendientes.set(p));
    }
  }

  /** Al elegir el procedimiento, se precargan áreas, padrino (el autor) y versión express. */
  elegir(id: number): void {
    const p = this.pendientes().find((x) => x.id === id) ?? null;
    this.elegido.set(p);
    if (p) {
      this.form.patchValue({
        areaIds: p.areas.map((a) => a.id),
        padrinoId: p.autorId,
        versionExpress: p.versionExpress ?? '',
      });
    }
  }

  casar(): void {
    const v = this.form.getRawValue();
    this.guardando.set(true);
    this.api
      .casar({
        procedimientoId: v.procedimientoId!,
        fecha: v.fecha || null,
        votos: v.votos ?? '',
        versionExpress: v.versionExpress ?? '',
        oficianteId: v.oficianteId!,
        padrinoId: v.padrinoId ?? null,
        areaIds: v.areaIds ?? [],
      })
      .subscribe({
        next: (c) => {
          this.notificador.ok(`¡Vivan los novios! ${c.codigo} se casó${c.grupal ? ' (grupal)' : ''}.`);
          this.form.patchValue({ procedimientoId: null, votos: '', versionExpress: '', areaIds: [] });
          this.elegido.set(null);
          this.guardando.set(false);
          this.cargar();
        },
        error: (e) => {
          this.notificador.error(e);
          this.guardando.set(false);
        },
      });
  }
}
