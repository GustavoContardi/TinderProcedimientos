import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { Avatar, avatar } from '../../core/diseno';
import { mensajeDeError } from '../../core/ui.service';

interface Demo {
  email: string;
  nombre: string;
  etq: string;
  av: Avatar;
}

/** Usuarios del seed (backend/src/main/resources/db/changelog/seed/seed.sql). Clave de todos: demo123. */
const DEMOS: Demo[] = [
  { email: 'facundo@demo.com', nombre: 'Facundo Ibarra', etq: 'Empleado · Desarrollo', av: avatar({ nombre: 'Facundo Ibarra', rol: 'EMPLEADO', areaId: 2 }) },
  { email: 'ana@demo.com', nombre: 'Ana Paredes', etq: 'Empleada · Operaciones y Mesa de Ayuda', av: avatar({ nombre: 'Ana Paredes', rol: 'EMPLEADO', areaId: 1 }) },
  { email: 'calidad@demo.com', nombre: 'Mónica Calderón', etq: 'Calidad · gestiona ciclos y ceremonias', av: avatar({ nombre: 'Mónica Calderón', rol: 'CALIDAD' }) },
  { email: 'direccion@demo.com', nombre: 'Ricardo Ferreyra', etq: 'Dirección · oficia y firma actas', av: avatar({ nombre: 'Ricardo Ferreyra', rol: 'DIRECCION' }) },
];
const CLAVE_DEMO = 'demo123';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <form class="login" (ngSubmit)="entrar(email, pass)">
      <div class="anillos"></div>
      <div class="kicker">Defensoría del Pueblo · ISO 9001</div>
      <h1>Tinder de procedimientos</h1>
      <p class="lema">Hasta que la burocracia nos separe</p>

      <div class="campos">
        <input type="email" name="email" placeholder="Email institucional" autocomplete="username" [(ngModel)]="email" />
        <input type="password" name="pass" placeholder="Contraseña" autocomplete="current-password" [(ngModel)]="pass" />
        @if (error()) {
          <div class="error">{{ error() }}</div>
        }
        <button type="submit" class="btn btn-petroleo entrar" [disabled]="cargando()">{{ cargando() ? 'Entrando…' : 'Entrar' }}</button>
      </div>

      <div class="separador"><span></span>Usuarios de prueba · clave {{ claveDemo }}<span></span></div>
      <div class="demos">
        @for (d of demos; track d.email) {
          <button type="button" class="demo" (click)="entrar(d.email, claveDemo)" [disabled]="cargando()">
            <span class="av" [style.background]="d.av.bg" [style.color]="d.av.color">{{ d.av.ini }}</span>
            <span class="flex quien"><span class="n">{{ d.nombre }}</span><span class="e">{{ d.etq }}</span></span>
            <span class="ms">arrow_forward</span>
          </button>
        }
      </div>
    </form>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .login { flex: 1; overflow-y: auto; display: flex; flex-direction: column; padding: 60px 26px 28px; }
    .kicker { margin-top: 30px; font-size: 11px; font-weight: 600; letter-spacing: .16em; text-transform: uppercase; color: var(--gris); }
    h1 { margin: 10px 0 0; font-family: var(--serif); font-weight: 500; font-size: 46px; line-height: .98; letter-spacing: -.03em; color: var(--petroleo); }
    .lema { margin: 12px 0 0; font-family: var(--serif); font-style: italic; font-size: 20px; color: #9A5B12; }
    .campos { display: flex; flex-direction: column; gap: 10px; margin-top: 34px; }
    input {
      width: 100%; padding: 14px 16px; border-radius: 14px; border: 1px solid rgba(18,52,59,.16);
      background: var(--papel); color: var(--texto); outline: none; font-size: 15px;
    }
    input:focus { border-color: var(--petroleo); box-shadow: var(--foco); }
    .error { font-size: 13.5px; color: var(--rechazo-txt); font-weight: 600; }
    .entrar { font-size: 16px; }
    .separador { display: flex; align-items: center; gap: 12px; margin: 30px 0 12px; font-size: 12px; color: var(--gris); }
    .separador span { flex: 1; height: 1px; background: rgba(18,52,59,.12); }
    .demos { display: flex; flex-direction: column; gap: 8px; }
    .demo {
      display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 16px;
      border: 1px solid rgba(18,52,59,.09); background: var(--papel); cursor: pointer; text-align: left;
    }
    .demo:hover { border-color: rgba(18,52,59,.3); }
    .av { width: 40px; height: 40px; border-radius: 50%; display: grid; place-items: center; font-weight: 600; font-size: 14px; flex: none; }
    .quien { display: flex; flex-direction: column; gap: 2px; }
    .n { font-weight: 600; font-size: 15px; color: var(--petroleo); }
    .e { font-size: 12.5px; color: var(--gris); }
    .demo .ms { font-size: 20px; color: var(--gris-claro); }
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly demos = DEMOS;
  readonly claveDemo = CLAVE_DEMO;
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  email = '';
  pass = '';

  constructor() {
    if (this.auth.logueado()) {
      this.router.navigate(['/votar']);
    }
  }

  entrar(email: string, pass: string): void {
    if (!email.trim() || !pass) {
      this.error.set('Completá email y contraseña (o elegí un usuario de prueba).');
      return;
    }
    this.cargando.set(true);
    this.error.set(null);
    this.auth.login(email, pass).subscribe({
      next: () => this.router.navigate(['/votar']),
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }
}
