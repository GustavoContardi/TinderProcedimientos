import { Component, HostListener, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CdkDrag, CdkDragEnd, CdkDragMove } from '@angular/cdk/drag-drop';
import { forkJoin } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { Ciclo, Perfil, ValorVoto } from '../../core/models';
import { NotificadorService } from '../../core/notificador.service';

/** Distancia (px) a partir de la cual un arrastre cuenta como voto. */
const UMBRAL_SWIPE = 120;
const DURACION_SALIDA_MS = 320;
const DURACION_MATCH_MS = 1500;

@Component({
  selector: 'app-swipe',
  imports: [CdkDrag],
  templateUrl: './swipe.component.html',
  styleUrl: './swipe.component.scss',
})
export class SwipeComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly notificador = inject(NotificadorService);
  private timers: ReturnType<typeof setTimeout>[] = [];

  readonly cargando = signal(true);
  readonly ciclo = signal<Ciclo | null>(null);
  readonly mazo = signal<Perfil[]>([]);
  readonly votados = signal(0);
  readonly dx = signal(0);
  readonly salida = signal<'der' | 'izq' | null>(null);
  readonly votando = signal(false);
  readonly match = signal(false);

  readonly actual = computed(() => this.mazo()[0] ?? null);
  readonly siguiente = computed(() => this.mazo()[1] ?? null);

  /** Rotación según el desplazamiento; si está saliendo, la tarjeta vuela fuera de la pantalla. */
  readonly transformacion = computed(() => {
    const salida = this.salida();
    if (salida) {
      const signo = salida === 'der' ? 1 : -1;
      return `translateX(${signo * 120}vw) rotate(${signo * 30}deg)`;
    }
    const grados = Math.max(-20, Math.min(20, this.dx() / 12));
    return `rotate(${grados}deg)`;
  });
  readonly opacidadSirve = computed(() => (this.salida() === 'der' ? 1 : Math.max(0, Math.min(1, this.dx() / UMBRAL_SWIPE))));
  readonly opacidadSobra = computed(() => (this.salida() === 'izq' ? 1 : Math.max(0, Math.min(1, -this.dx() / UMBRAL_SWIPE))));

  ngOnInit(): void {
    this.cargar();
  }

  ngOnDestroy(): void {
    this.timers.forEach(clearTimeout);
  }

  cargar(): void {
    this.cargando.set(true);
    forkJoin({ ciclo: this.api.cicloActual(), mazo: this.api.mazo() }).subscribe({
      next: ({ ciclo, mazo }) => {
        this.ciclo.set(ciclo);
        this.mazo.set(mazo);
        this.cargando.set(false);
      },
      error: (e) => {
        this.notificador.error(e);
        this.cargando.set(false);
      },
    });
  }

  mover(e: CdkDragMove): void {
    this.dx.set(e.distance.x);
  }

  soltar(e: CdkDragEnd): void {
    const x = e.distance.x;
    if (x > UMBRAL_SWIPE) {
      this.votar('LIKE', e);
    } else if (x < -UMBRAL_SWIPE) {
      this.votar('NOPE', e);
    } else {
      e.source.reset();
      this.dx.set(0);
    }
  }

  @HostListener('window:keydown', ['$event'])
  teclado(e: KeyboardEvent): void {
    if (e.key === 'ArrowRight') {
      e.preventDefault();
      this.votar('LIKE');
    } else if (e.key === 'ArrowLeft') {
      e.preventDefault();
      this.votar('NOPE');
    }
  }

  votar(valor: ValorVoto, arrastre?: CdkDragEnd): void {
    const perfil = this.actual();
    if (!perfil || this.votando()) {
      arrastre?.source.reset();
      return;
    }
    this.votando.set(true);
    this.api.votar(perfil.id, valor).subscribe({
      next: (r) => {
        this.salida.set(valor === 'LIKE' ? 'der' : 'izq');
        if (r.match) {
          this.mostrarMatch();
        }
        this.despues(DURACION_SALIDA_MS, () => this.siguienteTarjeta());
      },
      error: (e: HttpErrorResponse) => {
        this.notificador.error(e);
        if (e.status === 409 || e.status === 422 || e.status === 403) {
          // Ya votado o fuera de juego: lo sacamos del mazo igual.
          this.siguienteTarjeta();
        } else {
          arrastre?.source.reset();
          this.dx.set(0);
          this.votando.set(false);
        }
      },
    });
  }

  private siguienteTarjeta(): void {
    this.mazo.update((m) => m.slice(1));
    this.votados.update((n) => n + 1);
    this.salida.set(null);
    this.dx.set(0);
    this.votando.set(false);
  }

  private mostrarMatch(): void {
    this.match.set(true);
    this.despues(DURACION_MATCH_MS, () => this.match.set(false));
  }

  private despues(ms: number, fn: () => void): void {
    this.timers.push(setTimeout(fn, ms));
  }
}
