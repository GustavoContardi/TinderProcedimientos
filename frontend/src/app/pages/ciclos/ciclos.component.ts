import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Alerta, Ciclo, ResumenCierre } from '../../core/models';
import { NotificadorService } from '../../core/notificador.service';
import { EstadoChipComponent } from '../../shared/estado-chip.component';

@Component({
  selector: 'app-ciclos',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, EstadoChipComponent],
  template: `
    <section class="pagina">
      <header>
        <h1>🗓️ Ciclos de votación</h1>
        <p>Un solo ciclo abierto a la vez. Al cerrarlo se calculan los resultados y cada procedimiento sigue su destino.</p>
      </header>

      @if (actual(); as c) {
        <article class="tarjeta-blanca abierto">
          <div class="fila">
            <div>
              <h2>Ciclo {{ c.nombre }} <span class="chip veredicto-match">ABIERTO</span></h2>
              <p>Del {{ c.fechaInicio | date: 'd/M/yyyy' }} al {{ c.fechaFin | date: 'd/M/yyyy' }}</p>
            </div>
            <span class="espaciador"></span>
            <button mat-flat-button class="cerrar" (click)="cerrar(c)" [disabled]="trabajando()">
              {{ trabajando() ? 'Contando votos…' : 'Cerrar ciclo y calcular resultados' }}
            </button>
          </div>
        </article>
      } @else {
        <form class="tarjeta-blanca" [formGroup]="form" (ngSubmit)="abrir()">
          <h2>Abrir un ciclo nuevo</h2>
          <div class="fila">
            <mat-form-field appearance="outline"><mat-label>Nombre</mat-label><input matInput formControlName="nombre" placeholder="2027-T1" /></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Inicio</mat-label><input matInput type="date" formControlName="fechaInicio" /></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Fin</mat-label><input matInput type="date" formControlName="fechaFin" /></mat-form-field>
          </div>
          <button mat-flat-button type="submit" [disabled]="form.invalid || trabajando()">Abrir ciclo 🔥</button>
        </form>
      }

      @if (resumen(); as r) {
        <article class="tarjeta-blanca resumen">
          <h2>Resultado del cierre de {{ r.ciclo.nombre }}</h2>
          <p>{{ r.resultadosCalculados }} resultados por área calculados. <a routerLink="/resultados">Ver resultados →</a></p>
          <ul>
            @for (t of r.transiciones; track t.procedimientoId) {
              <li>
                <strong>{{ t.codigo }}</strong> {{ t.nombre }}
                <div class="trans"><app-estado-chip [estado]="t.desde" /> → <app-estado-chip [estado]="t.hacia" /></div>
                <small>{{ t.mensaje }}</small>
              </li>
            }
          </ul>
        </article>
      }

      <article class="tarjeta-blanca">
        <h2>🚨 Alertas para Calidad</h2>
        @for (a of alertas(); track a.id) {
          <div class="alerta" [class.revisada]="a.revisada">
            <span class="tipo">{{ iconoAlerta(a) }}</span>
            <div class="texto">
              <strong>{{ a.codigo }} · ciclo {{ a.cicloNombre }}</strong>
              <p>{{ a.mensaje }}</p>
            </div>
            @if (!a.revisada) { <button mat-button (click)="revisar(a)">Marcar revisada</button> }
          </div>
        } @empty {
          <p class="vacio">Sin dramas por ahora.</p>
        }
      </article>

      <article class="tarjeta-blanca">
        <h2>Historial</h2>
        @for (c of ciclos(); track c.id) {
          <div class="historial">
            <strong>{{ c.nombre }}</strong>
            <span>{{ c.fechaInicio | date: 'd/M/yyyy' }} – {{ c.fechaFin | date: 'd/M/yyyy' }}</span>
            <span [class]="'chip ' + (c.estado === 'ABIERTO' ? 'veredicto-match' : 'veredicto-quorum')">{{ c.estado }}</span>
          </div>
        }
      </article>
    </section>
  `,
  styles: `
    .abierto { border-left: 6px solid var(--match); }
    .abierto p { margin: 0; color: #5b6b6e; }
    .cerrar { --mdc-filled-button-container-color: var(--rechazo); flex: 0 0 auto; }
    .resumen { border-left: 6px solid var(--ambar); }
    .resumen ul { list-style: none; padding: 0; margin: 0; }
    .resumen li { padding: 8px 0; border-bottom: 1px solid #eef0f0; }
    .trans { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin: 4px 0; }
    .resumen small { color: #5b6b6e; }
    .alerta { display: flex; gap: 10px; align-items: flex-start; padding: 8px 0; border-bottom: 1px solid #eef0f0; }
    .alerta.revisada { opacity: .5; }
    .alerta .tipo { font-size: 1.5rem; }
    .alerta .texto { flex: 1; }
    .alerta p { margin: 2px 0 0; font-size: .9rem; }
    .historial { display: flex; gap: 12px; align-items: center; padding: 6px 0; flex-wrap: wrap; }
    .historial span:nth-child(2) { color: #6b7a7d; flex: 1; }
  `,
})
export class CiclosComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notificador = inject(NotificadorService);

  readonly actual = signal<Ciclo | null>(null);
  readonly ciclos = signal<Ciclo[]>([]);
  readonly alertas = signal<Alerta[]>([]);
  readonly resumen = signal<ResumenCierre | null>(null);
  readonly trabajando = signal(false);

  readonly form = inject(FormBuilder).nonNullable.group({
    nombre: ['', Validators.required],
    fechaInicio: ['', Validators.required],
    fechaFin: ['', Validators.required],
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.api.cicloActual().subscribe((c) => this.actual.set(c));
    this.api.ciclos().subscribe((c) => this.ciclos.set(c));
    this.api.alertas().subscribe((a) => this.alertas.set(a));
  }

  cerrar(c: Ciclo): void {
    if (!confirm(`¿Cerrar el ciclo ${c.nombre}? Se calculan los resultados y no se puede volver atrás.`)) return;
    this.trabajando.set(true);
    this.api.cerrarCiclo(c.id).subscribe({
      next: (r) => {
        this.resumen.set(r);
        this.notificador.ok(`Ciclo ${c.nombre} cerrado. ¡Que empiecen los casamientos y velorios!`);
        this.trabajando.set(false);
        this.cargar();
      },
      error: (e) => {
        this.notificador.error(e);
        this.trabajando.set(false);
      },
    });
  }

  abrir(): void {
    this.trabajando.set(true);
    this.api.abrirCiclo(this.form.getRawValue()).subscribe({
      next: (c) => {
        this.notificador.ok(`Ciclo ${c.nombre} abierto. A deslizar.`);
        this.form.reset();
        this.resumen.set(null);
        this.trabajando.set(false);
        this.cargar();
      },
      error: (e) => {
        this.notificador.error(e);
        this.trabajando.set(false);
      },
    });
  }

  revisar(a: Alerta): void {
    this.api.revisarAlerta(a.id).subscribe({ next: () => this.cargar(), error: (e) => this.notificador.error(e) });
  }

  iconoAlerta(a: Alerta): string {
    return { RELACION_A_DISTANCIA: '✈️', OBLIGATORIO_NO_VELABLE: '⚖️', CANDIDATO_CASAMIENTO_GRUPAL: '💍' }[a.tipo];
  }
}
