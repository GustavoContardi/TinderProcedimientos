import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/notificador.service';

const DEMO = [
  { email: 'facundo@demo.com', etiqueta: 'Empleado (Desarrollo)' },
  { email: 'ana@demo.com', etiqueta: 'Empleada (Operaciones)' },
  { email: 'calidad@demo.com', etiqueta: 'Calidad' },
  { email: 'direccion@demo.com', etiqueta: 'Dirección' },
];

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <div class="fondo">
      <form class="caja" [formGroup]="form" (ngSubmit)="entrar()">
        <div class="logo">💍</div>
        <h1>Tinder de procedimientos</h1>
        <p class="lema">Hasta que la burocracia nos separe</p>

        <mat-form-field appearance="outline">
          <mat-label>Email</mat-label>
          <input matInput type="email" formControlName="email" autocomplete="username" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Contraseña</mat-label>
          <input matInput type="password" formControlName="password" autocomplete="current-password" />
        </mat-form-field>

        @if (error()) {
          <p class="error">{{ error() }}</p>
        }

        <button mat-flat-button type="submit" class="entrar" [disabled]="form.invalid || cargando()">
          {{ cargando() ? 'Entrando…' : 'Entrar' }}
        </button>

        <div class="demo">
          <span>Usuarios de prueba (clave <code>demo123</code>):</span>
          <div class="chips">
            @for (d of demo; track d.email) {
              <button type="button" (click)="usar(d.email)">{{ d.etiqueta }}</button>
            }
          </div>
        </div>
      </form>
    </div>
  `,
  styles: `
    .fondo {
      min-height: 100vh; display: grid; place-items: center; padding: 16px; box-sizing: border-box;
      background: radial-gradient(circle at 20% 10%, #1d4b54, var(--petroleo) 60%);
    }
    .caja {
      width: 100%; max-width: 380px; background: var(--papel); border-radius: 20px;
      padding: 28px 22px; box-shadow: 0 20px 50px rgba(0,0,0,.35); text-align: center;
    }
    .logo { font-size: 3.5rem; animation: latido 1.6s ease-in-out infinite; }
    h1 { font-size: 1.6rem; margin-bottom: 0; }
    .lema { font-family: var(--fuente-titulos); font-style: italic; color: var(--ambar); margin: 4px 0 20px; }
    .entrar { width: 100%; height: 48px; font-size: 1.05rem; }
    .error { color: var(--rechazo); font-weight: 700; margin: 0 0 12px; }
    .demo { margin-top: 20px; font-size: .85rem; color: #5b6b6e; }
    .chips { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; margin-top: 8px; }
    .chips button {
      border: 1px solid #cfd8da; background: #fff; border-radius: 999px; padding: 6px 10px;
      cursor: pointer; font: inherit; color: var(--petroleo);
    }
    .chips button:hover { border-color: var(--ambar); background: #fff7ea; }
    @keyframes latido { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.12); } }
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly demo = DEMO;
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  constructor() {
    if (this.auth.logueado()) {
      this.router.navigate(['/swipe']);
    }
  }

  usar(email: string): void {
    this.form.setValue({ email, password: 'demo123' });
    this.error.set(null);
  }

  entrar(): void {
    if (this.form.invalid) return;
    const { email, password } = this.form.getRawValue();
    this.cargando.set(true);
    this.error.set(null);
    this.auth.login(email, password).subscribe({
      next: () => this.router.navigate(['/swipe']),
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }
}
