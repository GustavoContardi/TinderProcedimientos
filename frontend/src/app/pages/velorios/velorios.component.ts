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
import { Procedimiento, Velorio } from '../../core/models';
import { NotificadorService } from '../../core/notificador.service';

@Component({
  selector: 'app-velorios',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <section class="pagina">
      <header>
        <h1>🕯️ Velorios</h1>
        <p>Los rechazados se dan de baja con obituario y acta. Nunca se borran: quedan como documento obsoleto (ISO 9001 7.5.3).</p>
      </header>

      @if (esCalidad()) {
        <form class="tarjeta-blanca registro" [formGroup]="form" (ngSubmit)="crear()">
          <h2>Organizar velorio</h2>
          @if (sinVelorio().length === 0) {
            <p class="vacio">No hay procedimientos pendientes de velorio sin organizar.</p>
          } @else {
            <div class="fila">
              <mat-form-field appearance="outline">
                <mat-label>Difunto</mat-label>
                <mat-select formControlName="procedimientoId" (selectionChange)="sugerir($event.value)">
                  @for (p of sinVelorio(); track p.id) { <mat-option [value]="p.id">{{ p.codigo }} {{ p.nombre }}</mat-option> }
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Fecha</mat-label>
                <input matInput type="date" formControlName="fecha" />
              </mat-form-field>
            </div>
            <mat-form-field appearance="outline">
              <mat-label>Causa de muerte</mat-label>
              <input matInput formControlName="causaMuerte" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Epitafio (editable)</mat-label>
              <textarea matInput rows="2" formControlName="epitafio"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Deudos</mat-label>
              <input matInput formControlName="deudos" />
            </mat-form-field>
            @if (minutos() !== null) { <p class="minutos">⏱️ Nos robaba {{ minutos() }} minutos por semana.</p> }
            <button mat-flat-button type="submit" [disabled]="form.invalid || guardando()">Organizar velorio 🕯️</button>
          }
        </form>
      }

      <div class="grilla">
        @for (v of velorios(); track v.id) {
          <article class="lapida">
            <span class="cruz">✝</span>
            <h3>{{ v.codigo }} {{ v.nombre }}</h3>
            <p>{{ v.fechaNacimiento | date: 'yyyy' }} – {{ v.fecha | date: 'yyyy' }}</p>
            <p class="epitafio">{{ v.epitafio }}</p>
            <p class="acta" [class.firmada]="v.actaFirmada">{{ v.actaFirmada ? '✔ Acta firmada' : '✍️ Acta pendiente de firma' }}</p>
            <a mat-button [routerLink]="['/velorios', v.id]">Ver necrológica →</a>
          </article>
        } @empty {
          <div class="vacio tarjeta-blanca">Nadie murió todavía. Por ahora.</div>
        }
      </div>
    </section>
  `,
  styles: `
    .registro { border-top: 6px solid #333; }
    .minutos { color: var(--rechazo); font-weight: 700; }
    .grilla { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
    .lapida {
      background: linear-gradient(#3a3f41, #25292a); color: #e8e8e8; text-align: center;
      border-radius: 90px 90px 12px 12px; padding: 22px 16px 12px; box-shadow: 0 8px 20px rgba(0,0,0,.25);
    }
    .lapida h3 { color: #f2f2f2; font-size: 1.05rem; }
    .lapida p { margin: 4px 0; font-size: .85rem; color: #bfc6c8; }
    .lapida a { color: var(--ambar); }
    .cruz { font-size: 1.6rem; color: var(--ambar); }
    .epitafio { font-style: italic; font-family: var(--fuente-titulos); }
    .acta { color: var(--ambar) !important; }
    .acta.firmada { color: #9fd3cb !important; }
  `,
})
export class VeloriosComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly notificador = inject(NotificadorService);

  readonly esCalidad = computed(() => this.auth.tieneRol('CALIDAD'));
  readonly velorios = signal<Velorio[]>([]);
  readonly pendientes = signal<Procedimiento[]>([]);
  readonly minutos = signal<number | null>(null);
  readonly guardando = signal(false);
  readonly sinVelorio = computed(() => {
    const velados = new Set(this.velorios().map((v) => v.procedimientoId));
    return this.pendientes().filter((p) => !velados.has(p.id));
  });

  readonly form = inject(FormBuilder).nonNullable.group({
    procedimientoId: [0, Validators.min(1)],
    fecha: [hoyIso()],
    causaMuerte: ['', Validators.required],
    epitafio: ['', Validators.required],
    deudos: ['nadie', Validators.required],
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.api.velorios().subscribe({ next: (v) => this.velorios.set(v), error: (e) => this.notificador.error(e) });
    if (this.esCalidad()) {
      this.api.procedimientos('PENDIENTE_VELORIO').subscribe((p) => this.pendientes.set(p));
    }
  }

  /** Trae causa de muerte y epitafio sugeridos (el usuario los puede editar). */
  sugerir(id: number): void {
    this.api.sugerenciaVelorio(id).subscribe({
      next: (s) => {
        this.form.patchValue({ causaMuerte: s.causaMuerte, epitafio: s.epitafio, deudos: s.deudos });
        this.minutos.set(s.minutosSemanalesPerdidos);
      },
      error: (e) => this.notificador.error(e),
    });
  }

  crear(): void {
    const v = this.form.getRawValue();
    this.guardando.set(true);
    this.api.crearVelorio({ ...v, fecha: v.fecha || null }).subscribe({
      next: (vel) => {
        this.notificador.ok(`Velorio de ${vel.codigo} organizado. Falta la firma de la Dirección.`);
        this.form.reset({ procedimientoId: 0, fecha: hoyIso(), causaMuerte: '', epitafio: '', deudos: 'nadie' });
        this.minutos.set(null);
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
