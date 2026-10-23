import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ApiService } from '../../core/api.service';
import { hoyIso } from '../../core/etiquetas';
import { Area, Procedimiento, ProcedimientoRequest, TipoProcedimiento, Usuario } from '../../core/models';
import { mensajeDeError } from '../../core/notificador.service';

@Component({
  selector: 'app-procedimiento-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data ? 'Editar ' + data.codigo : 'Nuevo procedimiento' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form">
        <div class="fila">
          <mat-form-field appearance="outline">
            <mat-label>Código</mat-label>
            <input matInput formControlName="codigo" placeholder="PR-07" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Tipo</mat-label>
            <mat-select formControlName="tipo">
              <mat-option value="PROCEDIMIENTO">Procedimiento</mat-option>
              <mat-option value="REGISTRO">Registro</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Bio (en primera persona, con onda)</mat-label>
          <textarea matInput rows="3" formControlName="bio"></textarea>
        </mat-form-field>
        <div class="fila">
          <mat-form-field appearance="outline">
            <mat-label>Fecha de creación</mat-label>
            <input matInput type="date" formControlName="fechaCreacion" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Minutos por uso</mat-label>
            <input matInput type="number" min="0" formControlName="minutosPorUso" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Usos por semana</mat-label>
            <input matInput type="number" min="0" formControlName="usosPorSemana" />
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Te doy… (beneficio)</mat-label>
          <input matInput formControlName="beneficio" />
        </mat-form-field>
        <div class="fila">
          <mat-form-field appearance="outline">
            <mat-label>Áreas</mat-label>
            <mat-select formControlName="areaIds" multiple>
              @for (a of areas(); track a.id) { <mat-option [value]="a.id">{{ a.nombre }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Autor (padrino)</mat-label>
            <mat-select formControlName="autorId">
              <mat-option [value]="null">Nadie se hace cargo</mat-option>
              @for (u of usuarios(); track u.id) { <mat-option [value]="u.id">{{ u.nombre }}</mat-option> }
            </mat-select>
          </mat-form-field>
        </div>
        <mat-checkbox formControlName="obligatorio">Obligatorio por norma (no se puede velar)</mat-checkbox>
        @if (form.controls.obligatorio.value) {
          <mat-form-field appearance="outline">
            <mat-label>Base de obligatoriedad</mat-label>
            <input matInput formControlName="baseObligatoriedad" placeholder="ISO 9001 7.5.3 / Ley ..." />
          </mat-form-field>
        }
        <mat-form-field appearance="outline">
          <mat-label>Versión express (acuerdo prenupcial)</mat-label>
          <textarea matInput rows="2" formControlName="versionExpress"></textarea>
        </mat-form-field>
        @if (error()) { <p class="error">{{ error() }}</p> }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button (click)="guardar()" [disabled]="form.invalid || guardando()">Guardar</button>
    </mat-dialog-actions>
  `,
  styles: `
    .form { display: flex; flex-direction: column; gap: 4px; padding-top: 8px; }
    .fila { gap: 12px; }
    .error { color: var(--rechazo); font-weight: 700; }
  `,
})
export class ProcedimientoDialogComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly ref = inject(MatDialogRef<ProcedimientoDialogComponent>);
  readonly data = inject<Procedimiento | null>(MAT_DIALOG_DATA);

  readonly areas = signal<Area[]>([]);
  readonly usuarios = signal<Usuario[]>([]);
  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = inject(FormBuilder).group({
    codigo: ['', [Validators.required, Validators.maxLength(20)]],
    nombre: ['', Validators.required],
    tipo: ['PROCEDIMIENTO' as TipoProcedimiento, Validators.required],
    bio: ['', Validators.required],
    fechaCreacion: [hoyIso(), Validators.required],
    minutosPorUso: [10, [Validators.required, Validators.min(0)]],
    usosPorSemana: [1, [Validators.required, Validators.min(0)]],
    beneficio: ['', Validators.required],
    obligatorio: [false],
    baseObligatoriedad: [''],
    autorId: [null as number | null],
    versionExpress: [''],
    areaIds: [[] as number[], Validators.required],
  });

  ngOnInit(): void {
    this.api.areas().subscribe((a) => this.areas.set(a));
    this.api.usuarios().subscribe((u) => this.usuarios.set(u));
    const p = this.data;
    if (p) {
      this.form.patchValue({
        ...p,
        baseObligatoriedad: p.baseObligatoriedad ?? '',
        versionExpress: p.versionExpress ?? '',
        areaIds: p.areas.map((a) => a.id),
      });
    }
  }

  guardar(): void {
    const v = this.form.getRawValue();
    const body: ProcedimientoRequest = {
      codigo: v.codigo ?? '',
      nombre: v.nombre ?? '',
      tipo: v.tipo ?? 'PROCEDIMIENTO',
      bio: v.bio ?? '',
      fechaCreacion: v.fechaCreacion ?? hoyIso(),
      minutosPorUso: Number(v.minutosPorUso ?? 0),
      usosPorSemana: Number(v.usosPorSemana ?? 0),
      beneficio: v.beneficio ?? '',
      obligatorio: !!v.obligatorio,
      baseObligatoriedad: v.obligatorio ? v.baseObligatoriedad || null : null,
      autorId: v.autorId ?? null,
      versionExpress: v.versionExpress || null,
      areaIds: v.areaIds ?? [],
    };
    this.guardando.set(true);
    const op = this.data ? this.api.actualizarProcedimiento(this.data.id, body) : this.api.crearProcedimiento(body);
    op.subscribe({
      next: () => this.ref.close(true),
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.guardando.set(false);
      },
    });
  }
}
