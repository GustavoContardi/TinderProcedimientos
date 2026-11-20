import {
  Alerta,
  CategoriaObjecion,
  DecisionSegundaCita,
  EstadoCasamiento,
  EstadoMejora,
  EstadoProcedimiento,
  OrigenMejora,
  Rol,
  TipoProcedimiento,
  Veredicto,
} from './models';

/** [texto, fondo, tinta] para pintar chips. */
export type Colores = readonly [string, string, string];

export const ESTADOS: Record<EstadoProcedimiento, Colores> = {
  BORRADOR: ['Borrador', '#ECE8E0', '#4F5958'],
  EN_TINDER: ['En votación', '#F8E1DB', '#8A2F1D'],
  SEGUNDA_CITA: ['Segunda cita', '#FBE9CC', '#6B4300'],
  PENDIENTE_CASAMIENTO: ['Pendiente de casamiento', '#DCEDEA', '#1D5A52'],
  CASADO: ['Casado', '#2E7D73', '#FFFFFF'],
  PENDIENTE_VELORIO: ['Pendiente de velorio', '#3B3F40', '#F2F2F2'],
  VELADO: ['Velado', '#151819', '#B9BFC1'],
};
export const ESTADOS_LISTA = Object.keys(ESTADOS) as EstadoProcedimiento[];

/** Destinos y veredictos: casamiento verde, segunda cita ámbar, velorio coral, insuficientes gris. */
export const VEREDICTOS: Record<Veredicto, Colores> = {
  CASAMIENTO: ['Casamiento', '#2E7D73', '#FFFFFF'],
  SEGUNDA_CITA: ['Segunda cita', '#F2A541', '#3D2600'],
  VELORIO: ['Velorio', '#D0573F', '#FFFFFF'],
  QUORUM_INSUFICIENTE: ['Quórum insuficiente', '#E3E1DC', '#4F5958'],
  PARTICIPACION_INSUFICIENTE: ['Participación insuficiente', '#E3E1DC', '#4F5958'],
};

export const ESTADOS_CASAMIENTO: Record<EstadoCasamiento, Colores> = {
  VIGENTE: ['Vigente', '#DCEDEA', '#1D5A52'],
  POSTERGADO: ['Postergado', '#FBE9CC', '#6B4300'],
  RENOVADO: ['Renovado', '#ECE8E0', '#4F5958'],
  DIVORCIADO: ['Divorciado', '#F8E1DB', '#8A2F1D'],
};

/** [título, ícono, fondo, tinta] */
export const ALERTAS: Record<Alerta['tipo'], readonly [string, string, string, string]> = {
  RELACION_A_DISTANCIA: ['Relación a distancia', 'social_distance', '#FBE9CC', '#6B4300'],
  OBLIGATORIO_NO_VELABLE: ['Obligatorio no velable', 'gavel', '#F8E1DB', '#8A2F1D'],
  CANDIDATO_CASAMIENTO_GRUPAL: ['Casamiento grupal', 'diversity_3', '#DCEDEA', '#1D5A52'],
};

export function iconoTipo(t: TipoProcedimiento): string {
  return t === 'REGISTRO' ? 'assignment' : 'menu_book';
}

/** Líneas guía de Resultados: mismos umbrales que backend/src/main/resources/application.yml (tinder.*). */
export const UMBRAL_CASAMIENTO = 70;
export const UMBRAL_VELORIO = 40;

export const CATEGORIAS: Record<CategoriaObjecion, string> = {
  DUPLICA_OTRO_REGISTRO: 'Duplica otro registro',
  DEMASIADOS_PASOS: 'Demasiados pasos',
  NO_APLICA_A_MI_TAREA: 'No aplica a mi tarea',
  DESACTUALIZADO: 'Está desactualizado',
  FALTA_ALGO: 'Le falta algo',
  NO_SE_PUEDE_EN_URGENCIAS: 'No se puede en urgencias',
  OTRO: 'Otro motivo',
};
export const CATEGORIAS_LISTA = Object.keys(CATEGORIAS) as CategoriaObjecion[];

/** [texto, ícono] */
export const ORIGENES_MEJORA: Record<OrigenMejora, readonly [string, string]> = {
  OBJECION: ['Objeción', 'record_voice_over'],
  SEGUNDA_CITA: ['Segunda cita', 'coffee'],
  ACTA_DEFUNCION: ['Acta de defunción', 'local_florist'],
  ANIVERSARIO: ['Aniversario', 'cake'],
  REFORMULACION: ['Reformulación', 'restart_alt'],
};
export const ORIGENES_MEJORA_LISTA = Object.keys(ORIGENES_MEJORA) as OrigenMejora[];

export const ESTADOS_MEJORA: Record<EstadoMejora, Colores> = {
  NUEVA: ['Nueva', '#ECE8E0', '#4F5958'],
  EN_ANALISIS: ['En análisis', '#FBE9CC', '#6B4300'],
  EN_CURSO: ['En curso', '#DCEDEA', '#1D5A52'],
  RESUELTA: ['Resuelta', '#2E7D73', '#FFFFFF'],
  DESCARTADA: ['Descartada', '#F8E1DB', '#8A2F1D'],
};
export const ESTADOS_MEJORA_LISTA = Object.keys(ESTADOS_MEJORA) as EstadoMejora[];

/** [título, explicación] */
export const DECISIONES: Record<DecisionSegundaCita, readonly [string, string]> = {
  AJUSTAR_Y_REVOTAR: ['Ajustar y revotar', 'El dueño lo edita (versión + 1) y se vuelve a votar: ya, en una votación rápida, o en el próximo ciclo trimestral.'],
  CASAR_CON_CONDICIONES: ['Casar con condiciones', 'Votación rápida de 7 días: con 70 % o más se casa y las condiciones van a la libreta.'],
  VELORIO: ['Velorio', 'Votación rápida de 7 días: con menos de 40 % pasa a pendiente de velorio.'],
};

// ---------- Personas ----------

export interface Avatar {
  ini: string;
  bg: string;
  color: string;
  etq: string;
}

export function avatar(u: { nombre: string; rol: Rol; areaId?: number; areaNombre?: string }): Avatar {
  const palabras = u.nombre.split(/\s+/).filter((w) => w && !/^dra?\.?$/i.test(w));
  const ini = palabras
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('');
  if (u.rol === 'JUEZ_PAZ') {
    return { ini, bg: '#12343B', color: '#F5F0E6', etq: `Juez de paz · ${u.areaNombre ?? 'coordina todo'}` };
  }
  if (u.rol === 'DIRECCION') {
    return { ini, bg: '#F8E1DB', color: '#8A2F1D', etq: 'Dirección · padrino institucional' };
  }
  const par = (u.areaId ?? 0) % 2 === 0;
  return {
    ini,
    bg: par ? '#DCEDEA' : '#FBE9CC',
    color: par ? '#1D5A52' : '#6B4300',
    etq: `Empleado/a · ${u.areaNombre ?? ''}`,
  };
}

// ---------- Fechas (siempre ISO yyyy-mm-dd, sin zonas horarias) ----------

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
const MESES_CORTOS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

function partes(iso: string): { y: number; m: number; d: number } {
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number);
  return { y, m, d };
}

export function hoyIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** 6/11/2026 */
export function fCorta(iso: string): string {
  const x = partes(iso);
  return `${x.d}/${x.m}/${x.y}`;
}

/** 6 de noviembre de 2026 */
export function fLarga(iso: string): string {
  const x = partes(iso);
  return `${x.d} de ${MESES[x.m - 1]} de ${x.y}`;
}

/** 31 de diciembre */
export function fDiaMes(iso: string): string {
  const x = partes(iso);
  return `${x.d} de ${MESES[x.m - 1]}`;
}

export function anio(iso: string): number {
  return partes(iso).y;
}

/** 1 oct → 31 dic 2026 */
export function fRangoLargo(desde: string, hasta: string): string {
  const a = partes(desde);
  const b = partes(hasta);
  const anioA = a.y === b.y ? '' : ` ${a.y}`;
  return `${a.d} ${MESES_CORTOS[a.m - 1]}${anioA} → ${b.d} ${MESES_CORTOS[b.m - 1]} ${b.y}`;
}

/** 1/7 – 30/9/2026 */
export function fRangoCorto(desde: string, hasta: string): string {
  const a = partes(desde);
  const b = partes(hasta);
  const anioA = a.y === b.y ? '' : `/${a.y}`;
  return `${a.d}/${a.m}${anioA} – ${b.d}/${b.m}/${b.y}`;
}

/** "2026-07-12T10:30:00" → 12/7/2026 a las 10:30 */
export function fFirma(isoFechaHora: string): string {
  const hora = isoFechaHora.slice(11, 16);
  return hora ? `${fCorta(isoFechaHora)} a las ${hora}` : fCorta(isoFechaHora);
}

export function diasEntre(desde: string, hasta: string): number {
  return Math.round((Date.parse(hasta.slice(0, 10)) - Date.parse(desde.slice(0, 10))) / 864e5);
}

/** Trimestre que sigue al último ciclo (o el trimestre actual si no hubo ninguno). */
export function siguienteTrimestre(ultimoFin: string | null): { nombre: string; fechaInicio: string; fechaFin: string } {
  let y: number;
  let m: number;
  if (ultimoFin) {
    const x = partes(ultimoFin);
    const dia = new Date(Date.UTC(x.y, x.m - 1, x.d + 1));
    y = dia.getUTCFullYear();
    m = dia.getUTCMonth() + 1;
  } else {
    const hoy = partes(hoyIso());
    y = hoy.y;
    m = hoy.m;
  }
  const q = Math.floor((m - 1) / 3) + 1;
  const mesInicio = 3 * q - 2;
  const ultimoDia = new Date(Date.UTC(y, 3 * q, 0)).getUTCDate();
  const dos = (n: number) => String(n).padStart(2, '0');
  return {
    nombre: `${y}-T${q}`,
    fechaInicio: `${y}-${dos(mesInicio)}-01`,
    fechaFin: `${y}-${dos(3 * q)}-${dos(ultimoDia)}`,
  };
}

/** ["A","B","C"] → "A, B y C" */
export function lista(a: string[]): string {
  return a.length < 2 ? a.join('') : `${a.slice(0, -1).join(', ')} y ${a[a.length - 1]}`;
}
