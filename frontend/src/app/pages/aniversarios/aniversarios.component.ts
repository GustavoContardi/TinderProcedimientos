import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../core/api.service';
import { Casamiento } from '../../core/models';
import { NotificadorService } from '../../core/notificador.service';

@Component({
  selector: 'app-aniversarios',
  imports: [DatePipe, RouterLink, MatButtonModule],
  template: `
    <section class="pagina">
      <header>
        <h1>🎂 Aniversarios</h1>
        <p>Casamientos con aniversario en los próximos 15 días (o ya vencido). ¿Renuevan los votos o se divorcian?</p>
      </header>

      @for (c of lista(); track c.id) {
        <article class="tarjeta-blanca aniversario">
          <div class="info">
            <h2>{{ c.codigo }} {{ c.nombre }} @if (c.grupal) { 💍💍 }</h2>
            <p>Casado el {{ c.fecha | date: 'd/M/yyyy' }} con {{ c.areas.join(', ') }}.</p>
            <p class="cuando" [class.vencido]="c.diasParaAniversario < 0">
              @if (c.diasParaAniversario > 0) { Aniversario en {{ c.diasParaAniversario }} días ({{ c.fechaAniversario | date: 'd/M' }}) }
              @else if (c.diasParaAniversario === 0) { ¡El aniversario es hoy! }
              @else { Aniversario vencido hace {{ -c.diasParaAniversario }} días }
            </p>
            @if (c.obligatorio) { <p class="nota">⚖️ Es obligatorio: si se divorcian vuelve a segunda cita, no al velorio.</p> }
            <a [routerLink]="['/casamientos', c.id]">Ver libreta</a>
          </div>
          <div class="acciones">
            <button mat-flat-button class="renovar" (click)="renovar(c)">💍 Renovar votos</button>
            <button mat-stroked-button class="divorciar" (click)="divorciar(c)">💔 Divorciar</button>
          </div>
        </article>
      } @empty {
        <div class="vacio tarjeta-blanca"><span class="emoji">🥂</span>No hay aniversarios cerca. Todo el mundo feliz.</div>
      }
    </section>
  `,
  styles: `
    .aniversario { display: flex; gap: 16px; flex-wrap: wrap; align-items: center; border-left: 6px solid var(--ambar); }
    .info { flex: 1 1 320px; }
    .info h2 { font-size: 1.15rem; }
    .info p { margin: 2px 0; }
    .cuando { font-weight: 700; color: var(--match); }
    .cuando.vencido { color: var(--rechazo); }
    .nota { font-size: .85rem; color: #8a5a00; }
    .acciones { display: flex; gap: 8px; flex-wrap: wrap; }
    .renovar { --mdc-filled-button-container-color: var(--match); }
    .divorciar { color: var(--rechazo); }
  `,
})
export class AniversariosComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notificador = inject(NotificadorService);
  readonly lista = signal<Casamiento[]>([]);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.api.aniversarios().subscribe({ next: (l) => this.lista.set(l), error: (e) => this.notificador.error(e) });
  }

  renovar(c: Casamiento): void {
    if (!confirm(`¿Renovar los votos de ${c.codigo} por 6 meses más?`)) return;
    this.api.renovar(c.id).subscribe({
      next: (n) => {
        this.notificador.ok(`${c.codigo} renovó sus votos. Próximo aniversario: ${n.fechaAniversario}`);
        this.cargar();
      },
      error: (e) => this.notificador.error(e),
    });
  }

  divorciar(c: Casamiento): void {
    const destino = c.obligatorio ? 'segunda cita' : 'velorio';
    if (!confirm(`¿Divorciar a ${c.codigo}? Pasa a ${destino}.`)) return;
    this.api.divorciar(c.id).subscribe({
      next: () => {
        this.notificador.ok(`${c.codigo} se divorció. Destino: ${destino}.`);
        this.cargar();
      },
      error: (e) => this.notificador.error(e),
    });
  }
}
