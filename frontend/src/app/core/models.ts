export type Rol = 'EMPLEADO' | 'JUEZ_PAZ' | 'DIRECCION';
export type ValorVoto = 'LIKE' | 'NOPE';
export type TipoProcedimiento = 'PROCEDIMIENTO' | 'REGISTRO';
export type EstadoProcedimiento =
  | 'BORRADOR'
  | 'EN_TINDER'
  | 'SEGUNDA_CITA'
  | 'PENDIENTE_CASAMIENTO'
  | 'CASADO'
  | 'PENDIENTE_VELORIO'
  | 'VELADO';
export type Veredicto = 'CASAMIENTO' | 'SEGUNDA_CITA' | 'VELORIO' | 'QUORUM_INSUFICIENTE' | 'PARTICIPACION_INSUFICIENTE';
export type EstadoCasamiento = 'VIGENTE' | 'POSTERGADO' | 'RENOVADO' | 'DIVORCIADO';
export type TipoCiclo = 'TRIMESTRAL' | 'RAPIDA';
export type CategoriaObjecion =
  | 'DUPLICA_OTRO_REGISTRO'
  | 'DEMASIADOS_PASOS'
  | 'NO_APLICA_A_MI_TAREA'
  | 'DESACTUALIZADO'
  | 'FALTA_ALGO'
  | 'NO_SE_PUEDE_EN_URGENCIAS'
  | 'OTRO';
export type OrigenObjecion = 'VOTO' | 'BODA' | 'ANIVERSARIO';
export type EstadoObjecion = 'PENDIENTE' | 'EVALUADA' | 'RESPONDIDA';
export type DecisionSegundaCita = 'AJUSTAR_Y_REVOTAR' | 'CASAR_CON_CONDICIONES' | 'VELORIO';
export type OrigenMejora = 'OBJECION' | 'SEGUNDA_CITA' | 'ACTA_DEFUNCION' | 'ANIVERSARIO' | 'REFORMULACION';
export type EstadoMejora = 'NUEVA' | 'EN_ANALISIS' | 'EN_CURSO' | 'RESUELTA' | 'DESCARTADA';

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  rol: Rol;
  areaId: number;
  areaNombre: string;
}

export interface Area {
  id: number;
  nombre: string;
}

export interface Perfil {
  id: number;
  codigo: string;
  nombre: string;
  tipo: TipoProcedimiento;
  bio: string;
  edadAnios: number;
  minutosPorUso: number;
  beneficio: string;
  usoTexto: string;
  padrinoNombre: string | null;
  duenoNombre: string | null;
  version: number;
  obligatorio: boolean;
  baseObligatoriedad: string | null;
  areas: string[];
  cicloId: number;
  cicloNombre: string;
  votacionRapida: boolean;
}

export interface ObjecionNueva {
  categoria: CategoriaObjecion;
  texto: string | null;
}

export interface Ciclo {
  id: number;
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  estado: 'ABIERTO' | 'CERRADO';
  tipo: TipoCiclo;
  procedimientoId: number | null;
  procedimientoCodigo: string | null;
}

export interface Transicion {
  procedimientoId: number;
  codigo: string;
  nombre: string;
  destino: Veredicto;
  desde: EstadoProcedimiento;
  hacia: EstadoProcedimiento;
  mensaje: string;
}

export interface ResumenCierre {
  ciclo: Ciclo;
  resultadosCalculados: number;
  transiciones: Transicion[];
}

export interface ResultadoArea {
  cicloId: number;
  cicloNombre: string;
  procedimientoId: number;
  codigo: string;
  nombre: string;
  areaId: number;
  areaNombre: string;
  veredicto: Veredicto;
  likes: number | null;
  nopes: number | null;
  total: number | null;
  pctLike: number | null;
  pctNope: number | null;
  mensaje: string;
}

export interface ResultadoProcedimiento {
  cicloId: number;
  cicloNombre: string;
  cicloTipo: TipoCiclo;
  procedimientoId: number;
  codigo: string;
  nombre: string;
  destino: Veredicto | null;
  pctGlobal: number | null;
  votantes: number | null;
  habilitados: number | null;
  participacionPct: number | null;
  grupal: boolean;
  relacionADistancia: boolean;
  mensaje: string;
  areas: ResultadoArea[];
}

export interface Procedimiento {
  id: number;
  codigo: string;
  nombre: string;
  tipo: TipoProcedimiento;
  bio: string;
  fechaCreacion: string;
  edadAnios: number;
  minutosPorUso: number;
  usosPorSemana: number;
  minutosSemanales: number;
  beneficio: string;
  obligatorio: boolean;
  baseObligatoriedad: string | null;
  autorId: number | null;
  padrinoNombre: string | null;
  duenoId: number | null;
  duenoNombre: string | null;
  version: number;
  versionExpress: string | null;
  estado: EstadoProcedimiento;
  reemplazaAId: number | null;
  reemplazaACodigo: string | null;
  segundasCitasConsecutivas: number;
  requiereReformulacion: boolean;
  editablePorDueno: boolean;
  areas: Area[];
}

export interface ProcedimientoRequest {
  codigo: string;
  nombre: string;
  tipo: TipoProcedimiento;
  bio: string;
  fechaCreacion: string;
  minutosPorUso: number;
  usosPorSemana: number;
  beneficio: string;
  obligatorio: boolean;
  baseObligatoriedad: string | null;
  autorId: number | null;
  duenoId: number | null;
  versionExpress: string | null;
  areaIds: number[];
}

export interface ContenidoRequest {
  nombre: string;
  bio: string;
  minutosPorUso: number;
  usosPorSemana: number;
  beneficio: string;
  versionExpress: string | null;
}

export interface Casamiento {
  id: number;
  numero: string;
  procedimientoId: number;
  codigo: string;
  nombre: string;
  obligatorio: boolean;
  versionProcedimiento: number;
  versionExpress: string | null;
  fecha: string;
  votos: string;
  padrinoNombre: string | null;
  duenoNombre: string | null;
  oficianteNombre: string;
  grupal: boolean;
  condiciones: string | null;
  informeAniversario: string | null;
  fechaAniversario: string;
  diasParaAniversario: number;
  estado: EstadoCasamiento;
  areas: string[];
  testigos: { id: number; nombre: string; areaNombre: string }[];
  resultados: { areaNombre: string; pct: number }[];
  objecionesTratadas: { categoria: CategoriaObjecion; origen: OrigenObjecion; fundada: boolean | null; respuesta: string | null }[];
}

export interface CasamientoRequest {
  procedimientoId: number;
  fecha: string | null;
  votos: string;
  versionExpress: string;
  oficianteId: number;
  padrinoId: number | null;
  duenoId: number | null;
  testigoIds: number[];
  condiciones: string | null;
  areaIds: number[];
}

export interface Aniversario {
  casamiento: Casamiento;
  objecionesAbiertas: number;
  mejorasAbiertas: Mejora[];
}

export interface Firma {
  rol: 'JUEZ_PAZ' | 'DIRECCION' | 'DUENO';
  etiqueta: string;
  usuarioId: number | null;
  nombre: string | null;
  fecha: string | null;
}

export interface Velorio {
  id: number;
  numeroActa: string;
  procedimientoId: number;
  codigo: string;
  nombre: string;
  version: number;
  fechaNacimiento: string;
  fecha: string;
  causa: string;
  epitafio: string;
  minutosSemanalesPerdidos: number;
  deudos: string;
  herencia: string | null;
  lecciones: string | null;
  reencarnacionId: number | null;
  reencarnacionCodigo: string | null;
  reencarnacionNombre: string | null;
  reencarnacionEstado: EstadoProcedimiento | null;
  sinReemplazoMotivo: string | null;
  duenoId: number | null;
  duenoNombre: string | null;
  firmas: Firma[];
  completa: boolean;
}

export interface VelorioRequest {
  procedimientoId: number;
  fecha: string | null;
  causa: string;
  epitafio: string;
  deudos: string;
  herencia: string | null;
  lecciones: string | null;
  reencarnacionId: number | null;
  nuevoReemplazo: { codigo: string; nombre: string } | null;
  sinReemplazoMotivo: string | null;
}

export interface SugerenciaVelorio {
  procedimientoId: number;
  codigo: string;
  nombre: string;
  causa: string;
  epitafio: string;
  minutosSemanalesPerdidos: number;
  deudos: string;
  duenoNombre: string | null;
  proximoNumeroActa: string;
}

export interface Archivo {
  procedimiento: Procedimiento;
  velorioId: number | null;
  numeroActa: string | null;
  fechaVelorio: string | null;
  reemplazoCodigo: string | null;
  reemplazoNombre: string | null;
}

export interface Objecion {
  id: string;
  procedimientoId: number;
  cicloNombre: string | null;
  origen: OrigenObjecion;
  categoria: CategoriaObjecion;
  texto: string | null;
  fundada: boolean | null;
  respuesta: string | null;
  estado: EstadoObjecion;
  mejoraId: number | null;
  mejoraCodigo: string | null;
}

export interface GrupoObjeciones {
  procedimientoId: number;
  codigo: string;
  nombre: string;
  estadoProcedimiento: EstadoProcedimiento;
  total: number;
  pendientes: number;
  categorias: { categoria: CategoriaObjecion; cantidad: number; objeciones: Objecion[] }[];
}

export interface SegundaCita {
  id: number;
  procedimiento: Procedimiento;
  cicloId: number | null;
  cicloNombre: string | null;
  fechaProgramada: string;
  estado: 'CONVOCADA' | 'REALIZADA';
  minuta: string | null;
  decision: DecisionSegundaCita | null;
  condiciones: string | null;
  cicloRapido: Ciclo | null;
  pctGlobal: number | null;
  votantes: number | null;
  habilitados: number | null;
  motivo: string | null;
}

export interface Mejora {
  id: number;
  codigo: string;
  origen: OrigenMejora;
  origenId: string | null;
  procedimientoId: number;
  procedimientoCodigo: string;
  procedimientoNombre: string;
  descripcion: string;
  responsableId: number;
  responsableNombre: string;
  fechaCompromiso: string;
  vencida: boolean;
  estado: EstadoMejora;
  respuesta: string | null;
}

export interface MejoraUpdate {
  descripcion: string;
  responsableId: number;
  fechaCompromiso: string;
  estado: EstadoMejora;
  respuesta: string | null;
}

export interface Celda {
  usuarioId: number;
  procedimientoId: number;
}

export interface Matriz {
  usuarios: Usuario[];
  procedimientos: { id: number; codigo: string; nombre: string; estado: EstadoProcedimiento; areas: string[] }[];
  celdas: Celda[];
}

export interface Designacion {
  id: number;
  usuarioId: number;
  usuarioNombre: string;
  areaId: number;
  areaNombre: string;
  desde: string;
  hasta: string | null;
  vigente: boolean;
}

export interface Calendario {
  ciclo: Ciclo | null;
  items: { semana: string; titulo: string; detalle: string; hecho: boolean }[];
}

export interface Alerta {
  id: number;
  procedimientoId: number;
  codigo: string;
  nombre: string;
  cicloNombre: string;
  tipo: 'RELACION_A_DISTANCIA' | 'OBLIGATORIO_NO_VELABLE' | 'CANDIDATO_CASAMIENTO_GRUPAL';
  mensaje: string;
  revisada: boolean;
  createdAt: string;
}

export interface ApiError {
  codigo: string;
  mensaje: string;
}
