import { Component, TemplateRef, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { CATEGORIAS, CATEGORIAS_LISTA, ESTADOS, ESTADOS_CASAMIENTO, ESTADOS_MEJORA, anio, fCorta, hoyIso, lista } from '../../core/diseno';
import {
  Aniversario,
  Archivo,
  Casamiento,
  CategoriaObjecion,
  Procedimiento,
  SugerenciaVelorio,
  Usuario,
  Velorio,
} from '../../core/models';
import { UiService } from '../../core/ui.service';

type Pestania = 'casamientos' | 'velorios' | 'aniversarios' | 'archivo';
type Reemplazo = 'existente' | 'borrador' | 'ninguno';

const ETIQUETAS: Record<Pestania, string> = {
  casamientos: 'Casamientos',
  velorios: 'Velorios',
  aniversarios: 'Aniversarios',
  archivo: 'Archivo',
};

@Component({
  selector: 'app-registro',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pagina">Registro civil</h1>
      <p class="subtitulo">Casamientos, velorios, aniversarios y el archivo de obsoletos.</p>
      <div class="segmentado">
        @for (t of pestanias(); track t) {
          <a [routerLink]="'/' + t" [class.on]="pestania() === t">{{ etiquetas[t] }}</a>
        }
      </div>

      <!-- Casamientos -->
      @if (pestania() === 'casamientos') {
        @if (esJuez() && pendCas().length) {
          <div class="rotulo">Esperando en el altar</div>
          <div class="lista" style="margin-bottom: 22px">
            @for (p of pendCas(); track p.id) {
              <div class="pendiente cas">
                <div>
                  <div class="codigo" style="color: #1D5A52">{{ p.codigo }} · v{{ p.version }} · {{ areasTxt(p) }}</div>
                  <div class="nombre-serif" style="font-size: 20px; margin-top: 2px">{{ p.nombre }}</div>
                </div>
                <button class="btn-chico btn-match" (click)="abrirCasar(p, hojaCasar)"><span class="ms">favorite</span>Registrar casamiento</button>
              </div>
            }
          </div>
        }
        <div class="rotulo">Libro de casamientos</div>
        <div class="lista">
          @for (c of casamientos(); track c.id) {
            <div class="tarjeta boda">
              @if (c.estado === 'POSTERGADO') {
                <div class="banner"><span class="ms">pause_circle</span>Boda postergada: una objeción fundada la frenó. Vuelve a segunda cita.</div>
              }
              <button class="boda-cuerpo" (click)="ui.abrirDoc('libreta', c.id)">
                <div class="anillos" style="--d: 28px; --b: 2.5px; --c2: #2E7D73; width: 42px"></div>
                <div class="flex" style="min-width: 0">
                  <div class="fila">
                    <span class="codigo">{{ c.numero }} · {{ c.codigo }}</span>
                    <span class="chip" [style.background]="ec(c)[1]" [style.color]="ec(c)[2]">{{ ec(c)[0] }}</span>
                  </div>
                  <div class="nombre-serif" style="margin-top: 3px">{{ c.nombre }}</div>
                  <div class="meta">Casado el {{ fCorta(c.fecha) }} con {{ lista(c.areas) }}{{ c.grupal ? ' (grupal)' : '' }}</div>
                </div>
                <span class="ms chevron">chevron_right</span>
              </button>
              @if (c.estado === 'VIGENTE') {
                <button class="objetar" (click)="abrirObjetar(c, hojaObjetar)"><span class="ms">front_hand</span>¿Alguien se opone?</button>
              }
            </div>
          } @empty {
            <div class="vacio-italico">Todavía no hubo casamientos.</div>
          }
        </div>
      }

      <!-- Velorios -->
      @if (pestania() === 'velorios') {
        @if (esJuez() && pendVel().length) {
          <div class="rotulo">Pendientes de velorio</div>
          <div class="lista" style="margin-bottom: 22px">
            @for (p of pendVel(); track p.id) {
              <div class="pendiente vel">
                <div>
                  <div class="codigo">{{ p.codigo }} · dueño: {{ p.duenoNombre ?? 'sin dueño' }}</div>
                  <div class="nombre-serif" style="font-size: 20px; margin-top: 2px">{{ p.nombre }}</div>
                </div>
                <button class="btn-chico btn-luto" (click)="abrirVelar(p, hojaVelar)"><span class="ms">local_florist</span>Labrar acta de defunción</button>
              </div>
            }
          </div>
        }
        <div class="rotulo">Actas de defunción</div>
        <div class="lista">
          @for (v of velorios(); track v.id) {
            <button class="lapida" (click)="ui.abrirDoc('necro', v.id)">
              <div class="fila">
                <span class="cruz">✝</span>
                <span class="codigo" style="color: #9AA3A5">Acta {{ v.numeroActa }} · {{ v.codigo }} · {{ anio(v.fechaNacimiento) }} – {{ anio(v.fecha) }}</span>
                <span class="flex"></span>
                <span class="chip" [style.background]="v.completa ? 'rgba(159,211,203,.16)' : '#F2A541'" [style.color]="v.completa ? '#9FD3CB' : '#3D2600'">
                  {{ firmadas(v) }}/3 firmas
                </span>
              </div>
              <div class="difunto">{{ v.nombre }}</div>
              <div class="epitafio">{{ v.epitafio }}</div>
              @if (v.reencarnacionCodigo) {
                <div class="reencarna"><span class="ms">autorenew</span>Reencarna en {{ v.reencarnacionCodigo }}</div>
              }
            </button>
          } @empty {
            <div class="vacio-italico">Nadie murió todavía. Por ahora.</div>
          }
        </div>
      }

      <!-- Aniversarios -->
      @if (pestania() === 'aniversarios') {
        <p class="intro-aniv">Casamientos que cumplen 6 meses en los próximos 15 días. El informe dice cómo les fue: ¿renuevan, segunda cita o divorcio?</p>
        <div class="lista" style="gap: 12px">
          @for (a of aniversarios(); track a.casamiento.id) {
            <div class="tarjeta aniv">
              <div class="fila" style="align-items: flex-start; gap: 14px">
                <div class="caja-dias" [class.vencido]="a.casamiento.diasParaAniversario < 0">
                  <div class="num">{{ a.casamiento.diasParaAniversario === 0 ? 'Hoy' : abs(a.casamiento.diasParaAniversario) }}</div>
                  <div class="unid">{{ a.casamiento.diasParaAniversario > 0 ? 'días' : a.casamiento.diasParaAniversario < 0 ? 'vencido' : '' }}</div>
                </div>
                <div class="flex" style="min-width: 0">
                  <div class="codigo">{{ a.casamiento.numero }} · {{ a.casamiento.codigo }}</div>
                  <div class="nombre-serif" style="margin-top: 2px">{{ a.casamiento.nombre }}</div>
                  <div class="meta" style="margin-top: 4px; line-height: 1.4">{{ metaAniv(a.casamiento) }}</div>
                </div>
              </div>
              <div class="informe">
                <div class="rotulo" style="margin: 0">Informe</div>
                <div class="dato"><span class="ms">record_voice_over</span>Objeciones abiertas: <strong>{{ a.objecionesAbiertas }}</strong></div>
                <div class="dato"><span class="ms">view_kanban</span>Mejoras abiertas: <strong>{{ a.mejorasAbiertas.length }}</strong></div>
                @for (m of a.mejorasAbiertas; track m.id) {
                  <div class="mejora-mini">
                    <span class="codigo">{{ m.codigo }}</span>
                    <span class="flex">{{ m.descripcion }}</span>
                    <span class="chip" [style.background]="em(m.estado)[1]" [style.color]="em(m.estado)[2]">{{ em(m.estado)[0] }}</span>
                  </div>
                }
                @if (esJuez()) {
                  <label class="campo" style="margin-top: 6px">
                    <span>Uso real</span>
                    <textarea rows="2" [(ngModel)]="usoReal[a.casamiento.id]" placeholder="¿Se usa? ¿Cuánto? ¿Se cumplió el prenup?"></textarea>
                  </label>
                }
              </div>
              @if (a.casamiento.obligatorio) {
                <div class="aviso-oblig"><span class="ms">gavel</span>Es obligatorio: si se divorcian vuelve a segunda cita, no al velorio.</div>
              }
              @if (esJuez()) {
                <div class="acciones">
                  <button class="btn btn-match" (click)="aniversario(a, 'renovar')" [disabled]="ocupado()">Renovar</button>
                  <button class="btn btn-ambar" (click)="aniversario(a, 'segunda-cita')" [disabled]="ocupado()">Segunda cita</button>
                  <button class="btn divorciar" (click)="aniversario(a, 'divorciar')" [disabled]="ocupado()">Divorciar</button>
                </div>
              } @else {
                <div class="meta">La Dirección revisa; las acciones las toma el juez de paz.</div>
              }
            </div>
          } @empty {
            <div class="vacio-italico">No hay aniversarios cerca. Todo el mundo feliz (o eso dicen).</div>
          }
        </div>
      }

      <!-- Archivo de obsoletos -->
      @if (pestania() === 'archivo') {
        <p class="intro-aniv">Procedimientos velados: obsoletos, de solo lectura. Nunca se borran (ISO 9001 7.5.3).</p>
        <div class="lista">
          @for (a of archivo(); track a.procedimiento.id) {
            <div class="tarjeta obsoleto">
              <div class="fila">
                <span class="codigo">{{ a.procedimiento.codigo }} · v{{ a.procedimiento.version }}</span>
                <span class="flex"></span>
                <span class="chip" [style.background]="estados.VELADO[1]" [style.color]="estados.VELADO[2]">Obsoleto</span>
              </div>
              <div class="nombre-serif" style="margin-top: 3px">{{ a.procedimiento.nombre }}</div>
              <div class="meta">
                @if (a.fechaVelorio) { Velado el {{ fCorta(a.fechaVelorio) }}. }
                {{ a.reemplazoCodigo ? 'Lo reemplaza ' + a.reemplazoCodigo + ' ' + a.reemplazoNombre + '.' : 'Sin reemplazo.' }}
              </div>
              @if (a.velorioId) {
                <button class="btn-texto ver-acta" (click)="ui.abrirDoc('necro', a.velorioId)"><span class="ms">description</span>Ver acta {{ a.numeroActa }}</button>
              }
            </div>
          } @empty {
            <div class="vacio-italico">El archivo está vacío: todavía no hay procedimientos velados.</div>
          }
        </div>
      }
    </div>

    <!-- Hoja: registrar casamiento -->
    <ng-template #hojaCasar>
      <div class="titulo-hoja">Registrar casamiento</div>
      <div class="form">
        <div class="campo">
          <span>Procedimiento</span>
          <div class="opciones">
            @for (p of pendCas(); track p.id) {
              <button class="pastilla chica" [class.on]="p.id === casarP()?.id" (click)="elegirCasar(p)">{{ p.codigo }} · {{ p.nombre }}</button>
            }
          </div>
        </div>
        @if (casarP(); as p) {
          @if (p.areas.length > 1) {
            <div class="grupal"><span class="ms">diversity_3</span>Casamiento grupal con {{ areasTxt(p) }}</div>
          }
          <label class="campo"><span>Votos</span><textarea rows="3" class="poetico" [(ngModel)]="fc.votos"></textarea></label>
          <label class="campo"><span>Acuerdo prenupcial · versión express</span><textarea rows="4" [(ngModel)]="fc.express"></textarea></label>
          <label class="campo"><span>Condiciones (si vienen de la segunda cita se copian solas)</span><textarea rows="2" [(ngModel)]="fc.condiciones" placeholder="Ej.: revisar el indicador en 3 meses"></textarea></label>
          <div class="dos">
            <label class="campo">
              <span>Dueño</span>
              <select [(ngModel)]="fc.duenoId">
                <option [ngValue]="null">Sin dueño</option>
                @for (u of noDireccion(); track u.id) { <option [ngValue]="u.id">{{ u.nombre }}</option> }
              </select>
            </label>
            <label class="campo">
              <span>Padrino (autor)</span>
              <select [(ngModel)]="fc.padrinoId">
                <option [ngValue]="null">Nadie se hace cargo</option>
                @for (u of usuarios(); track u.id) { <option [ngValue]="u.id">{{ u.nombre }}</option> }
              </select>
            </label>
          </div>
          <div class="campo">
            <span>Testigos · otras áreas que reciben el trabajo</span>
            <div class="opciones">
              @for (u of testigosPosibles(); track u.id) {
                <button class="pastilla chica" [class.on]="fc.testigoIds.includes(u.id)" (click)="alternarTestigo(u.id)">{{ u.nombre }} · {{ u.areaNombre }}</button>
              } @empty {
                <span class="meta">No hay gente de otras áreas: este casamiento va sin testigos.</span>
              }
            </div>
          </div>
          <label class="campo">
            <span>Oficiante (juez de paz)</span>
            <select [(ngModel)]="fc.oficianteId">
              @for (u of jueces(); track u.id) { <option [ngValue]="u.id">{{ u.nombre }} · {{ u.areaNombre }}</option> }
            </select>
          </label>
          <button class="btn btn-match" (click)="casar()" [disabled]="ocupado() || !fc.oficianteId">{{ ocupado() ? 'Casando…' : 'Sí, acepto · emitir libreta' }}</button>
        }
      </div>
    </ng-template>

    <!-- Hoja: ¿alguien se opone? -->
    <ng-template #hojaObjetar>
      <div class="titulo-hoja">¿Alguien se opone?</div>
      <p class="sub-hoja">Objeción anónima a la boda de {{ objetarC()?.codigo }}. Si el juez de paz la considera fundada, la boda se posterga.</p>
      <div class="form">
        <label class="campo">
          <span>Categoría</span>
          <select [(ngModel)]="fo.categoria">
            <option [ngValue]="null" disabled>Elegí una…</option>
            @for (c of categorias; track c) { <option [ngValue]="c">{{ etiquetasCat[c] }}</option> }
          </select>
        </label>
        <label class="campo"><span>¿Qué pasa?</span><textarea rows="3" maxlength="1000" [(ngModel)]="fo.texto"></textarea></label>
        <button class="btn btn-petroleo" (click)="objetar()" [disabled]="ocupado() || !fo.categoria">Me opongo (anónimo)</button>
        <button class="btn-texto" (click)="ui.cerrarHoja()">Que sean felices</button>
      </div>
    </ng-template>

    <!-- Hoja: acta de defunción -->
    <ng-template #hojaVelar>
      <div class="titulo-hoja">Acta de defunción{{ sug() ? ' N.º ' + sug()!.proximoNumeroActa : '' }}</div>
      <div class="form">
        <div class="campo">
          <span>Difunto</span>
          <div class="opciones">
            @for (p of pendVel(); track p.id) {
              <button class="pastilla chica" [class.on]="p.id === fv.pid" (click)="elegirVelar(p)">{{ p.codigo }} · {{ p.nombre }}</button>
            }
          </div>
        </div>
        <label class="campo"><span>Causa (sugerida)</span><textarea rows="2" [(ngModel)]="fv.causa"></textarea></label>
        <label class="campo"><span>Epitafio (sugerido)</span><input class="poetico" [(ngModel)]="fv.epitafio" /></label>
        <label class="campo"><span>Herencia · qué se rescata y adónde va</span><textarea rows="2" [(ngModel)]="fv.herencia" placeholder="Ej.: los datos útiles pasan al sistema de tickets"></textarea></label>
        <label class="campo"><span>Lecciones aprendidas</span><textarea rows="2" [(ngModel)]="fv.lecciones"></textarea></label>
        <div class="campo">
          <span>Reencarnación</span>
          <div class="segmentado" style="margin: 0">
            <button [class.on]="fv.reemplazo === 'existente'" (click)="fv.reemplazo = 'existente'">Existente</button>
            <button [class.on]="fv.reemplazo === 'borrador'" (click)="fv.reemplazo = 'borrador'">Crear borrador</button>
            <button [class.on]="fv.reemplazo === 'ninguno'" (click)="fv.reemplazo = 'ninguno'">Sin reemplazo</button>
          </div>
        </div>
        @switch (fv.reemplazo) {
          @case ('existente') {
            <label class="campo">
              <span>Lo reemplaza</span>
              <select [(ngModel)]="fv.reencarnacionId">
                <option [ngValue]="null" disabled>Elegí un procedimiento…</option>
                @for (p of vivos(); track p.id) { <option [ngValue]="p.id">{{ p.codigo }} · {{ p.nombre }}</option> }
              </select>
            </label>
          }
          @case ('borrador') {
            <div class="dos" style="grid-template-columns: 110px minmax(0, 1fr)">
              <label class="campo"><span>Código</span><input class="mono" [(ngModel)]="fv.codigoNuevo" placeholder="PR-11" maxlength="20" /></label>
              <label class="campo"><span>Nombre</span><input [(ngModel)]="fv.nombreNuevo" placeholder="Parte semanal en tickets" /></label>
            </div>
            <div class="dato-fijo"><span class="ms">edit_note</span><span>Nace en BORRADOR, con el mismo dueño y la misma matriz. Se crea la mejora "redactar el reemplazo".</span></div>
          }
          @case ('ninguno') {
            <label class="campo"><span>Motivo (obligatorio)</span><textarea rows="2" [(ngModel)]="fv.motivo" placeholder="¿Por qué no hace falta reemplazo?"></textarea></label>
          }
        }
        @if (sug(); as s) {
          <div class="dato-fijo"><span class="ms">draw</span><span>Firman: juez de paz, Dirección y el dueño (<strong>{{ s.duenoNombre ?? 'sin dueño' }}</strong>).</span></div>
        }
        @if (errorVel()) { <div class="error-form">{{ errorVel() }}</div> }
        <button class="btn btn-luto" (click)="velar()" [disabled]="ocupado() || !fv.pid">{{ ocupado() ? 'Labrando…' : 'Labrar acta' }}</button>
      </div>
    </ng-template>
  `,
  styles: `
    :host { flex: 1; min-height: 0; display: flex; flex-direction: column; }
    .segmentado a {
      flex: 1; height: 36px; border-radius: 10px; display: grid; place-items: center; text-decoration: none;
      color: var(--gris); font-size: 13px; font-weight: 600;
    }
    .segmentado a.on { background: var(--papel); color: var(--petroleo); box-shadow: 0 1px 3px rgba(18,52,59,.12); }
    .fila { display: flex; align-items: center; gap: 8px; }
    .meta { font-size: 12.5px; color: var(--gris); margin-top: 3px; }
    .pendiente { padding: 16px; border-radius: 18px; display: flex; flex-direction: column; gap: 12px; }
    .pendiente.cas { background: #E3EFEC; }
    .pendiente.vel { background: #E6E2DA; }
    .boda { overflow: hidden; }
    .boda-cuerpo { text-align: left; border: none; background: none; cursor: pointer; padding: 16px; display: flex; gap: 14px; align-items: center; width: 100%; }
    .banner { display: flex; gap: 8px; align-items: center; padding: 10px 14px; background: #FBE9CC; color: #6B4300; font-size: 12.5px; font-weight: 600; }
    .banner .ms { font-size: 18px; }
    .objetar {
      width: 100%; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 10px; border: none;
      border-top: 1px solid rgba(18,52,59,.07); background: transparent; color: var(--rechazo-txt); font-weight: 600; font-size: 13px; cursor: pointer;
    }
    .objetar .ms { font-size: 18px; }
    .chevron { font-size: 20px; color: var(--gris-claro); }
    .lapida {
      text-align: left; border: none; cursor: pointer; padding: 18px; border-radius: 18px; width: 100%;
      background: var(--luto); color: #E9E9E9; display: flex; flex-direction: column; gap: 8px;
    }
    .cruz { color: var(--ambar); font-size: 16px; }
    .difunto { font-family: var(--serif); font-size: 20px; color: #fff; }
    .epitafio { font-family: var(--serif); font-style: italic; font-size: 15px; line-height: 1.4; color: #F4E3C4; }
    .reencarna { display: flex; align-items: center; gap: 6px; font-size: 12.5px; color: #9FD3CB; }
    .reencarna .ms { font-size: 16px; }
    .intro-aniv { margin: 0 0 14px; font-size: 13.5px; line-height: 1.45; color: var(--texto-2); }
    .aniv { padding: 16px; display: flex; flex-direction: column; gap: 14px; }
    .caja-dias { flex: none; width: 64px; padding: 8px 0; border-radius: 14px; background: #DCEDEA; color: #1D5A52; text-align: center; }
    .caja-dias.vencido { background: #F8E1DB; color: #8A2F1D; }
    .caja-dias .num { font-family: var(--serif); font-size: 28px; line-height: 1; }
    .caja-dias .unid { font-size: 10.5px; font-weight: 600; letter-spacing: .04em; margin-top: 2px; }
    .informe { display: flex; flex-direction: column; gap: 6px; padding: 12px; border-radius: 14px; background: rgba(18,52,59,.04); }
    .dato { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--texto-2); }
    .dato .ms { font-size: 16px; color: var(--petroleo); }
    .dato strong { color: var(--petroleo); }
    .mejora-mini { display: flex; align-items: center; gap: 8px; font-size: 12.5px; color: var(--texto-2); padding-left: 22px; }
    .aviso-oblig { display: flex; gap: 6px; align-items: flex-start; font-size: 12px; line-height: 1.4; color: #7A4A0A; }
    .aviso-oblig .ms { font-size: 15px; }
    .acciones { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 6px; }
    .acciones .btn { height: 44px; border-radius: 12px; font-size: 13.5px; }
    .divorciar { border: 1px solid rgba(208,87,63,.5); background: transparent; color: var(--rechazo-txt); }
    .obsoleto { padding: 14px 16px; opacity: .92; }
    .ver-acta { display: inline-flex; align-items: center; gap: 6px; padding: 0; height: 36px; color: var(--petroleo); font-weight: 600; }
    .ver-acta .ms { font-size: 18px; }
    .opciones { display: flex; flex-wrap: wrap; gap: 6px; }
    .dos { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
    .grupal { display: flex; gap: 8px; align-items: center; padding: 10px 12px; border-radius: 12px; background: #FBE9CC; color: #5C3A05; font-size: 13px; }
    .grupal .ms { font-size: 18px; }
    .sub-hoja { margin: 6px 0 0; font-size: 13px; line-height: 1.45; color: var(--gris); }
  `,
})
export class RegistroComponent {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  readonly ui = inject(UiService);

  /** Viene de la ruta (data.pestania). */
  readonly pestania = input<Pestania>('casamientos');

  readonly fCorta = fCorta;
  readonly lista = lista;
  readonly anio = anio;
  readonly abs = Math.abs;
  readonly etiquetas = ETIQUETAS;
  readonly estados = ESTADOS;
  readonly categorias = CATEGORIAS_LISTA;
  readonly etiquetasCat = CATEGORIAS;

  readonly rol = computed(() => this.auth.usuario()?.rol);
  readonly esJuez = computed(() => this.rol() === 'JUEZ_PAZ');
  readonly pestanias = computed<Pestania[]>(() =>
    this.rol() === 'EMPLEADO' ? ['casamientos', 'velorios', 'archivo'] : ['casamientos', 'velorios', 'aniversarios', 'archivo'],
  );

  readonly casamientos = signal<Casamiento[]>([]);
  readonly velorios = signal<Velorio[]>([]);
  readonly aniversarios = signal<Aniversario[]>([]);
  readonly archivo = signal<Archivo[]>([]);
  readonly pendCas = signal<Procedimiento[]>([]);
  readonly vivos = signal<Procedimiento[]>([]);
  private readonly pendVelTodos = signal<Procedimiento[]>([]);
  readonly pendVel = computed(() => {
    const velados = new Set(this.velorios().map((v) => v.procedimientoId));
    return this.pendVelTodos().filter((p) => !velados.has(p.id));
  });
  readonly usuarios = signal<Usuario[]>([]);
  readonly jueces = computed(() => this.usuarios().filter((u) => u.rol === 'JUEZ_PAZ'));
  readonly noDireccion = computed(() => this.usuarios().filter((u) => u.rol !== 'DIRECCION'));
  readonly ocupado = signal(false);
  readonly errorVel = signal<string | null>(null);
  usoReal: Record<number, string> = {};

  readonly casarP = signal<Procedimiento | null>(null);
  readonly testigosPosibles = computed(() => {
    const p = this.casarP();
    if (!p) return [];
    const areas = new Set(p.areas.map((a) => a.id));
    return this.noDireccion().filter((u) => !areas.has(u.areaId));
  });
  fc = {
    votos: '',
    express: '',
    condiciones: '',
    padrinoId: null as number | null,
    duenoId: null as number | null,
    oficianteId: null as number | null,
    testigoIds: [] as number[],
  };

  readonly objetarC = signal<Casamiento | null>(null);
  fo = { categoria: null as CategoriaObjecion | null, texto: '' };

  readonly sug = signal<SugerenciaVelorio | null>(null);
  fv = {
    pid: 0,
    causa: '',
    epitafio: '',
    herencia: '',
    lecciones: '',
    reemplazo: 'borrador' as Reemplazo,
    reencarnacionId: null as number | null,
    codigoNuevo: '',
    nombreNuevo: '',
    motivo: '',
  };

  constructor() {
    // Recarga al entrar y cada vez que otra parte de la app cambia datos (p. ej. firmar un acta).
    effect(() => {
      this.ui.cambios();
      this.pestania();
      untracked(() => this.cargar());
    });
    if (this.esJuez()) {
      this.api.usuarios().subscribe((u) => this.usuarios.set(u));
    }
  }

  private cargar(): void {
    const err = (e: unknown) => this.ui.error(e);
    switch (this.pestania()) {
      case 'casamientos':
        this.api.casamientos().subscribe({ next: (c) => this.casamientos.set(c), error: err });
        if (this.esJuez()) this.api.procedimientos('PENDIENTE_CASAMIENTO').subscribe({ next: (p) => this.pendCas.set(p), error: err });
        break;
      case 'velorios':
        this.api.velorios().subscribe({ next: (v) => this.velorios.set(v), error: err });
        if (this.esJuez()) {
          this.api.procedimientos('PENDIENTE_VELORIO').subscribe({ next: (p) => this.pendVelTodos.set(p), error: err });
          this.api.procedimientos().subscribe({ next: (p) => this.vivos.set(p.filter((x) => x.estado !== 'PENDIENTE_VELORIO')), error: err });
        }
        break;
      case 'aniversarios':
        this.api.aniversarios().subscribe({ next: (a) => this.aniversarios.set(a), error: err });
        break;
      case 'archivo':
        this.api.archivo().subscribe({ next: (a) => this.archivo.set(a), error: err });
        break;
    }
  }

  areasTxt(p: Procedimiento): string {
    return lista(p.areas.map((a) => a.nombre));
  }

  ec(c: Casamiento) {
    return ESTADOS_CASAMIENTO[c.estado];
  }

  em(e: keyof typeof ESTADOS_MEJORA) {
    return ESTADOS_MEJORA[e];
  }

  firmadas(v: Velorio): number {
    return v.firmas.filter((f) => f.usuarioId).length;
  }

  metaAniv(c: Casamiento): string {
    const n = c.diasParaAniversario;
    const cuando = n < 0 ? `Aniversario vencido hace ${-n} días.` : `Aniversario el ${fCorta(c.fechaAniversario)}.`;
    return `Casado el ${fCorta(c.fecha)} con ${lista(c.areas)}. ${cuando}`;
  }

  // ---------- Casar ----------

  abrirCasar(p: Procedimiento, hoja: TemplateRef<unknown>): void {
    this.elegirCasar(p);
    this.ui.abrirHoja(hoja);
  }

  elegirCasar(p: Procedimiento): void {
    this.casarP.set(p);
    const yo = this.auth.usuario();
    this.fc = {
      votos: 'Prometemos usarte en las buenas, en las malas y en las auditorías.',
      express: p.versionExpress ?? '1. …\n2. …\n3. …',
      condiciones: '',
      padrinoId: p.autorId,
      duenoId: p.duenoId,
      oficianteId: yo?.rol === 'JUEZ_PAZ' ? yo.id : (this.jueces()[0]?.id ?? null),
      testigoIds: [],
    };
  }

  alternarTestigo(id: number): void {
    const ids = this.fc.testigoIds;
    this.fc.testigoIds = ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id];
  }

  casar(): void {
    const p = this.casarP();
    if (!p || !this.fc.oficianteId) return;
    this.ocupado.set(true);
    this.api
      .casar({
        procedimientoId: p.id,
        fecha: hoyIso(),
        votos: this.fc.votos,
        versionExpress: this.fc.express,
        oficianteId: this.fc.oficianteId,
        padrinoId: this.fc.padrinoId,
        duenoId: this.fc.duenoId,
        testigoIds: this.fc.testigoIds,
        condiciones: this.fc.condiciones.trim() || null,
        areaIds: p.areas.map((a) => a.id),
      })
      .subscribe({
        next: (c) => {
          this.ocupado.set(false);
          this.ui.abrirDoc('libreta', c.id);
          this.ui.ok(`${c.codigo} y su equipo: felices para siempre (o hasta el aniversario)`);
          this.ui.avisarCambio();
        },
        error: (e) => {
          this.ocupado.set(false);
          this.ui.error(e);
        },
      });
  }

  // ---------- ¿Alguien se opone? ----------

  abrirObjetar(c: Casamiento, hoja: TemplateRef<unknown>): void {
    this.objetarC.set(c);
    this.fo = { categoria: null, texto: '' };
    this.ui.abrirHoja(hoja);
  }

  objetar(): void {
    const c = this.objetarC();
    if (!c || !this.fo.categoria) return;
    this.ocupado.set(true);
    this.api.objetarCasamiento(c.id, { categoria: this.fo.categoria, texto: this.fo.texto.trim() || null }).subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.ui.cerrarHoja();
        this.ui.ok(r.mensaje);
      },
      error: (e) => {
        this.ocupado.set(false);
        this.ui.error(e);
      },
    });
  }

  // ---------- Velorio ----------

  abrirVelar(p: Procedimiento, hoja: TemplateRef<unknown>): void {
    this.elegirVelar(p);
    this.ui.abrirHoja(hoja);
  }

  /** Trae causa y epitafio sugeridos por el backend (editables). */
  elegirVelar(p: Procedimiento): void {
    this.errorVel.set(null);
    this.sug.set(null);
    this.fv = {
      pid: p.id,
      causa: '',
      epitafio: '',
      herencia: '',
      lecciones: '',
      reemplazo: 'borrador',
      reencarnacionId: null,
      codigoNuevo: '',
      nombreNuevo: '',
      motivo: '',
    };
    this.api.sugerenciaVelorio(p.id).subscribe({
      next: (s) => {
        if (this.fv.pid !== p.id) return;
        this.sug.set(s);
        this.fv.causa = s.causa;
        this.fv.epitafio = s.epitafio;
      },
      error: (e) => this.ui.error(e),
    });
  }

  velar(): void {
    const f = this.fv;
    if (!f.pid) return;
    if (f.reemplazo === 'existente' && !f.reencarnacionId) return this.errorVel.set('Elegí qué procedimiento lo reemplaza.');
    if (f.reemplazo === 'borrador' && (!f.codigoNuevo.trim() || !f.nombreNuevo.trim()))
      return this.errorVel.set('El borrador necesita código y nombre.');
    if (f.reemplazo === 'ninguno' && !f.motivo.trim()) return this.errorVel.set('Sin reemplazo hay que contar por qué.');
    this.errorVel.set(null);
    this.ocupado.set(true);
    this.api
      .crearVelorio({
        procedimientoId: f.pid,
        fecha: hoyIso(),
        causa: f.causa,
        epitafio: f.epitafio,
        deudos: 'ninguno',
        herencia: f.herencia.trim() || null,
        lecciones: f.lecciones.trim() || null,
        reencarnacionId: f.reemplazo === 'existente' ? f.reencarnacionId : null,
        nuevoReemplazo: f.reemplazo === 'borrador' ? { codigo: f.codigoNuevo.trim(), nombre: f.nombreNuevo.trim() } : null,
        sinReemplazoMotivo: f.reemplazo === 'ninguno' ? f.motivo.trim() : null,
      })
      .subscribe({
        next: (v) => {
          this.ocupado.set(false);
          this.ui.abrirDoc('necro', v.id);
          this.ui.ok(`Acta ${v.numeroActa} labrada. Faltan las tres firmas.`);
          this.ui.avisarCambio();
        },
        error: (e) => {
          this.ocupado.set(false);
          this.ui.error(e);
        },
      });
  }

  // ---------- Aniversarios ----------

  aniversario(a: Aniversario, accion: 'renovar' | 'segunda-cita' | 'divorciar'): void {
    const c = a.casamiento;
    this.ocupado.set(true);
    this.api.accionAniversario(c.id, accion, this.usoReal[c.id]?.trim() || null).subscribe({
      next: () => {
        this.ocupado.set(false);
        const textos = {
          renovar: 'Votos renovados por 6 meses más.',
          'segunda-cita': `${c.codigo} vuelve a segunda cita: hay cosas que hablar`,
          divorciar: c.obligatorio ? `${c.codigo} vuelve a segunda cita (es obligatorio)` : `${c.codigo} pasa a pendiente de velorio`,
        };
        this.ui.ok(textos[accion]);
        this.ui.avisarCambio();
      },
      error: (e) => {
        this.ocupado.set(false);
        this.ui.error(e);
      },
    });
  }
}
