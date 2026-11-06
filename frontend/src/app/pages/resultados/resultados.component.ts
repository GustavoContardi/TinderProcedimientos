import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { QUORUM_MINIMO, UMBRAL_MATCH, UMBRAL_RECHAZO, VEREDICTOS, fDiaMes } from '../../core/diseno';
import { Area, Ciclo, Resultado, Veredicto } from '../../core/models';
import { UiService } from '../../core/ui.service';

interface Fila {
  id: number;
  codigo: string;
  nombre: string;
  veredicto: Veredicto;
  pctLike: number;
  pctNope: number;
  total: number;
}

@Component({
  selector: 'app-resultados',
  template: `
    <div class="pantalla">
      <h1 class="titulo-pagina">Resultados</h1>
      <p class="subtitulo">{{ subtitulo() }}</p>

      @if (ciclos().length > 1) {
        <div class="pastillas">
          @for (c of ciclos(); track c.id) {
            <button class="pastilla" [class.on]="c.id === ciclo()?.id" (click)="elegirCiclo(c)">{{ c.nombre }}</button>
          }
        </div>
      }

      @if (esEmpleado()) {
        <div class="bloqueo">
          <span class="ms">lock</span>
          <span>Tu área: <strong>{{ auth.usuario()?.areaNombre }}</strong>. Solo ves resultados agregados, nunca votos individuales.</span>
        </div>
      } @else {
        <div class="pastillas">
          <button class="pastilla" [class.on]="areaId() === null" (click)="elegirArea(null)">Todas</button>
          @for (a of areas(); track a.id) {
            <button class="pastilla" [class.on]="areaId() === a.id" (click)="elegirArea(a.id)">{{ a.nombre }}</button>
          }
        </div>
      }

      <div class="conteos">
        @for (k of conteos(); track k.label) {
          <div class="tarjeta conteo">
            <div class="n">{{ k.n }}</div>
            <div class="l"><span class="punto" [style.background]="k.dot"></span>{{ k.label }}</div>
          </div>
        }
      </div>

      <div class="lista">
        @for (r of filas(); track r.id) {
          <div class="tarjeta resultado">
            <div class="sup">
              <div class="flex" style="min-width: 0">
                <div class="codigo">{{ r.codigo }}</div>
                <div class="nombre-serif" style="margin-top: 2px">{{ r.nombre }}</div>
              </div>
              <span class="chip veredicto" [style.background]="ver(r.veredicto)[1]" [style.color]="ver(r.veredicto)[2]">{{ ver(r.veredicto)[0] }}</span>
            </div>
            @if (r.veredicto !== 'QUORUM_INSUFICIENTE') {
              <div class="barra">
                <div [style.flex]="r.pctLike" class="si"></div>
                <div [style.flex]="r.pctNope" class="no"></div>
              </div>
              <div class="numeros">
                <span class="si">{{ r.pctLike }}% me sirve</span>
                <span>{{ r.total }} votos</span>
                <span class="no">{{ r.pctNope }}% me sobra</span>
              </div>
            } @else {
              <div class="barra rayada"></div>
              <div class="nota">Muy pocos votos: no mostramos números para cuidar el anonimato.</div>
            }
          </div>
        } @empty {
          @if (!cargando()) {
            <div class="vacio-italico">Todavía no hay resultados: se calculan cuando Calidad cierra el ciclo.</div>
          }
        }
      </div>
    </div>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .bloqueo { display: flex; align-items: center; gap: 10px; padding: 12px 14px; border-radius: 14px; background: rgba(18,52,59,.05); margin-bottom: 16px; }
    .bloqueo .ms { font-size: 20px; color: var(--petroleo); }
    .bloqueo span:last-child { font-size: 13px; line-height: 1.4; color: var(--texto-2); }
    .bloqueo strong { color: var(--petroleo); }
    .conteos { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin-bottom: 16px; }
    .conteo { padding: 12px; border-radius: 16px; }
    .conteo .n { font-family: var(--serif); font-size: 30px; line-height: 1; color: var(--petroleo); }
    .conteo .l { display: flex; align-items: center; gap: 6px; margin-top: 6px; font-size: 12px; color: var(--gris); }
    .punto { width: 8px; height: 8px; border-radius: 50%; flex: none; }
    .resultado { padding: 14px 16px; display: flex; flex-direction: column; gap: 10px; }
    .sup { display: flex; align-items: flex-start; gap: 10px; }
    .veredicto { flex: none; padding: 4px 10px; font-size: 12px; }
    .barra { display: flex; gap: 3px; height: 8px; }
    .barra > div { border-radius: 4px; }
    .barra .si { background: var(--match); }
    .barra .no { background: var(--rechazo); }
    .barra.rayada { border-radius: 4px; background: repeating-linear-gradient(135deg, #D9D5CC 0 4px, #ECE8E0 4px 8px); }
    .numeros { display: flex; justify-content: space-between; font-size: 12.5px; font-variant-numeric: tabular-nums; color: var(--gris); }
    .numeros .si { color: var(--match); font-weight: 600; }
    .numeros .no { color: var(--rechazo-txt); font-weight: 600; }
    .nota { font-size: 12.5px; line-height: 1.4; color: var(--gris); }
  `,
})
export class ResultadosComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly ui = inject(UiService);
  readonly auth = inject(AuthService);

  readonly ciclos = signal<Ciclo[]>([]);
  readonly ciclo = signal<Ciclo | null>(null);
  readonly areas = signal<Area[]>([]);
  readonly areaId = signal<number | null>(null);
  readonly resultados = signal<Resultado[]>([]);
  readonly cargando = signal(true);
  readonly esEmpleado = computed(() => this.auth.usuario()?.rol === 'EMPLEADO');

  readonly subtitulo = computed(() => {
    const c = this.ciclo();
    if (!c) return this.cargando() ? 'Cargando…' : 'Todavía no se cerró ningún ciclo.';
    const area = this.esEmpleado()
      ? this.auth.usuario()?.areaNombre
      : (this.areas().find((a) => a.id === this.areaId())?.nombre ?? 'todas las áreas');
    return `Ciclo ${c.nombre} · cerrado el ${fDiaMes(c.fechaFin)} · ${area}`;
  });

  /** Con un área elegida manda el veredicto del backend; con "Todas" se suman las áreas que tienen números. */
  readonly filas = computed<Fila[]>(() => {
    const porProc = new Map<number, Resultado[]>();
    for (const r of this.resultados()) {
      porProc.set(r.procedimientoId, [...(porProc.get(r.procedimientoId) ?? []), r]);
    }
    return [...porProc.values()].map((rs) => {
      const r0 = rs[0];
      if (rs.length === 1) {
        return {
          id: r0.procedimientoId,
          codigo: r0.codigo,
          nombre: r0.nombre,
          veredicto: r0.veredicto,
          pctLike: Math.round(r0.pctLike ?? 0),
          pctNope: Math.round(r0.pctNope ?? 0),
          total: r0.total ?? 0,
        };
      }
      const conNumeros = rs.filter((r) => r.total !== null);
      const likes = conNumeros.reduce((s, r) => s + (r.likes ?? 0), 0);
      const total = conNumeros.reduce((s, r) => s + (r.total ?? 0), 0);
      const pl = total ? likes / total : 0;
      const veredicto: Veredicto =
        total < QUORUM_MINIMO ? 'QUORUM_INSUFICIENTE' : pl >= UMBRAL_MATCH ? 'MATCH' : 1 - pl >= UMBRAL_RECHAZO ? 'RECHAZO' : 'SEGUNDA_CITA';
      const pctLike = Math.round(pl * 100);
      return { id: r0.procedimientoId, codigo: r0.codigo, nombre: r0.nombre, veredicto, pctLike, pctNope: 100 - pctLike, total };
    });
  });

  readonly conteos = computed(() => {
    const n = (v: Veredicto) => this.filas().filter((f) => f.veredicto === v).length;
    return [
      { n: n('MATCH'), label: 'Match', dot: '#2E7D73' },
      { n: n('SEGUNDA_CITA'), label: 'Segunda cita', dot: '#F2A541' },
      { n: n('RECHAZO'), label: 'Rechazo', dot: '#D0573F' },
    ];
  });

  ngOnInit(): void {
    if (this.esEmpleado()) {
      this.areaId.set(this.auth.usuario()?.areaId ?? null);
    } else {
      this.api.areas().subscribe((a) => this.areas.set(a));
    }
    this.api.ciclos().subscribe({
      next: (cs) => {
        const cerrados = cs.filter((c) => c.estado === 'CERRADO').sort((a, b) => b.fechaFin.localeCompare(a.fechaFin));
        this.ciclos.set(cerrados);
        this.ciclo.set(cerrados[0] ?? null);
        this.cargar();
      },
      error: (e) => {
        this.ui.error(e);
        this.cargando.set(false);
      },
    });
  }

  ver(v: Veredicto) {
    return VEREDICTOS[v];
  }

  elegirCiclo(c: Ciclo): void {
    this.ciclo.set(c);
    this.cargar();
  }

  elegirArea(id: number | null): void {
    this.areaId.set(id);
    this.cargar();
  }

  private cargar(): void {
    const c = this.ciclo();
    if (!c) {
      this.resultados.set([]);
      this.cargando.set(false);
      return;
    }
    this.cargando.set(true);
    this.api.resultados(c.id, this.areaId()).subscribe({
      next: (r) => {
        this.resultados.set(r);
        this.cargando.set(false);
      },
      error: (e) => {
        this.ui.error(e);
        this.cargando.set(false);
      },
    });
  }
}
