import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { UMBRAL_CASAMIENTO, UMBRAL_VELORIO, VEREDICTOS, fDiaMes } from '../../core/diseno';
import { Area, Ciclo, ResultadoProcedimiento, Veredicto } from '../../core/models';
import { UiService } from '../../core/ui.service';

@Component({
  selector: 'app-resultados',
  template: `
    <div class="pantalla">
      <h1 class="titulo-pagina">Resultados</h1>
      <p class="subtitulo">{{ subtitulo() }}</p>

      @if (ciclos().length > 1) {
        <div class="pastillas">
          @for (c of ciclos(); track c.id) {
            <button class="pastilla" [class.on]="c.id === ciclo()?.id" (click)="elegirCiclo(c)">
              @if (c.tipo === 'RAPIDA') { <span class="ms" style="font-size: 16px">bolt</span> }{{ c.nombre }}
            </button>
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

      <div class="leyenda">
        <span><i class="guia v"></i>{{ umbralVelorio }} % velorio</span>
        <span><i class="guia c"></i>{{ umbralCasamiento }} % casamiento</span>
        <span>Áreas con menos de 3 votos: sin números</span>
      </div>

      <div class="lista">
        @for (r of resultados(); track r.procedimientoId) {
          <div class="tarjeta resultado">
            <div class="sup">
              <div class="flex" style="min-width: 0">
                <div class="codigo">{{ r.codigo }}</div>
                <div class="nombre-serif" style="margin-top: 2px">{{ r.nombre }}</div>
              </div>
              @if (r.destino) {
                <span class="chip veredicto" [style.background]="ver(r.destino)[1]" [style.color]="ver(r.destino)[2]">{{ ver(r.destino)[0] }}</span>
              }
            </div>

            @if (r.grupal || r.relacionADistancia) {
              <div class="etiquetas">
                @if (r.grupal) { <span class="etiqueta grupal"><span class="ms">diversity_3</span>Casamiento grupal</span> }
                @if (r.relacionADistancia) { <span class="etiqueta distancia"><span class="ms">social_distance</span>Relación a distancia</span> }
              </div>
            }

            @if (r.votantes !== null) {
              <div class="participacion">
                <span class="ms">how_to_vote</span>
                Participación: <strong>{{ r.votantes }} / {{ r.habilitados }}</strong> de la matriz ({{ r.participacionPct }} %)
                @if (r.pctGlobal !== null) { · global <strong>{{ r.pctGlobal }} %</strong> me sirve }
              </div>
            }

            @for (a of r.areas; track a.areaId) {
              <div class="area">
                <div class="area-sup">
                  <span class="area-nombre">{{ a.areaNombre }}</span>
                  <span class="chip mini" [style.background]="ver(a.veredicto)[1]" [style.color]="ver(a.veredicto)[2]">{{ ver(a.veredicto)[0] }}</span>
                </div>
                @if (a.pctLike !== null) {
                  <div class="barra">
                    <div class="relleno" [style.width.%]="a.pctLike" [style.background]="ver(a.veredicto)[1]"></div>
                    <i class="guia v" [style.left.%]="umbralVelorio"></i>
                    <i class="guia c" [style.left.%]="umbralCasamiento"></i>
                  </div>
                  <div class="numeros"><span>{{ a.pctLike }} % me sirve</span><span>{{ a.total }} votos</span></div>
                } @else {
                  <div class="barra rayada"></div>
                  <div class="numeros"><span>Pocos votos: por anonimato no mostramos números.</span></div>
                }
              </div>
            }
            <div class="mensaje">{{ r.mensaje }}</div>
          </div>
        } @empty {
          @if (!cargando()) {
            <div class="vacio-italico">Todavía no hay resultados: se calculan cuando el juez de paz cierra el ciclo.</div>
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
    .conteos { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin-bottom: 12px; }
    .conteo { padding: 12px; border-radius: 16px; }
    .conteo .n { font-family: var(--serif); font-size: 30px; line-height: 1; color: var(--petroleo); }
    .conteo .l { display: flex; align-items: center; gap: 6px; margin-top: 6px; font-size: 12px; color: var(--gris); }
    .punto { width: 8px; height: 8px; border-radius: 50%; flex: none; }
    .leyenda { display: flex; flex-wrap: wrap; gap: 6px 14px; margin-bottom: 14px; font-size: 11.5px; color: var(--gris); }
    .leyenda span { display: inline-flex; align-items: center; gap: 6px; }
    .leyenda .guia { position: static; display: inline-block; height: 12px; }
    .resultado { padding: 14px 16px; display: flex; flex-direction: column; gap: 10px; }
    .sup { display: flex; align-items: flex-start; gap: 10px; }
    .veredicto { flex: none; padding: 4px 10px; font-size: 12px; }
    .etiquetas { display: flex; flex-wrap: wrap; gap: 6px; }
    .etiqueta { display: inline-flex; align-items: center; gap: 5px; padding: 4px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; }
    .etiqueta .ms { font-size: 16px; }
    .grupal { background: #DCEDEA; color: #1D5A52; }
    .distancia { background: #FBE9CC; color: #6B4300; }
    .participacion { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; font-size: 12.5px; color: var(--texto-2); }
    .participacion .ms { font-size: 16px; color: var(--petroleo); margin-right: 2px; }
    .participacion strong { color: var(--petroleo); }
    .area { display: flex; flex-direction: column; gap: 5px; padding-top: 8px; border-top: 1px solid rgba(18,52,59,.07); }
    .area-sup { display: flex; align-items: center; gap: 8px; }
    .area-nombre { flex: 1; font-size: 13px; font-weight: 600; color: var(--petroleo); }
    .chip.mini { padding: 2px 8px; font-size: 10.5px; }
    .barra { position: relative; height: 10px; border-radius: 5px; background: #ECE8E0; overflow: visible; }
    .barra .relleno { height: 100%; border-radius: 5px; }
    .barra.rayada { background: repeating-linear-gradient(135deg, #D9D5CC 0 4px, #ECE8E0 4px 8px); }
    .guia { position: absolute; top: -3px; width: 0; height: 16px; border-left: 2px dashed; }
    .guia.v { border-color: var(--rechazo); }
    .guia.c { border-color: var(--match); }
    .numeros { display: flex; justify-content: space-between; font-size: 12px; font-variant-numeric: tabular-nums; color: var(--gris); }
    .mensaje { font-size: 12.5px; line-height: 1.45; color: var(--texto-2); font-style: italic; }
  `,
})
export class ResultadosComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly ui = inject(UiService);
  readonly auth = inject(AuthService);
  readonly umbralCasamiento = UMBRAL_CASAMIENTO;
  readonly umbralVelorio = UMBRAL_VELORIO;

  readonly ciclos = signal<Ciclo[]>([]);
  readonly ciclo = signal<Ciclo | null>(null);
  readonly areas = signal<Area[]>([]);
  readonly areaId = signal<number | null>(null);
  readonly resultados = signal<ResultadoProcedimiento[]>([]);
  readonly cargando = signal(true);
  readonly esEmpleado = computed(() => this.auth.usuario()?.rol === 'EMPLEADO');

  readonly subtitulo = computed(() => {
    const c = this.ciclo();
    if (!c) return this.cargando() ? 'Cargando…' : 'Todavía no se cerró ningún ciclo.';
    const area = this.esEmpleado()
      ? this.auth.usuario()?.areaNombre
      : (this.areas().find((a) => a.id === this.areaId())?.nombre ?? 'todas las áreas');
    const tipo = c.tipo === 'RAPIDA' ? 'Votación rápida' : 'Ciclo';
    return `${tipo} ${c.nombre} · cerrado el ${fDiaMes(c.fechaFin)} · ${area}`;
  });

  readonly conteos = computed(() => {
    const n = (v: Veredicto) => this.resultados().filter((r) => r.destino === v).length;
    return [
      { n: n('CASAMIENTO'), label: 'Casamiento', dot: '#2E7D73' },
      { n: n('SEGUNDA_CITA'), label: 'Segunda cita', dot: '#F2A541' },
      { n: n('VELORIO'), label: 'Velorio', dot: '#D0573F' },
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
        const cerrados = cs.filter((c) => c.estado === 'CERRADO').sort((a, b) => b.fechaFin.localeCompare(a.fechaFin) || b.id - a.id);
        this.ciclos.set(cerrados);
        this.ciclo.set(cerrados.find((c) => c.tipo === 'TRIMESTRAL') ?? cerrados[0] ?? null);
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
    this.api.resultados(c.id, this.esEmpleado() ? null : this.areaId()).subscribe({
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
