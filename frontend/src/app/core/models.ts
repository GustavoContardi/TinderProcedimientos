export type Rol = 'EMPLEADO' | 'CALIDAD' | 'DIRECCION';
export type ValorVoto = 'LIKE' | 'NOPE';
export type TipoProcedimiento = 'PROCEDIMIENTO' | 'REGISTRO';
export type EstadoProcedimiento =
  | 'EN_TINDER'
  | 'SEGUNDA_CITA'
  | 'PENDIENTE_CASAMIENTO'
  | 'CASADO'
  | 'PENDIENTE_VELORIO'
  | 'VELADO';
export type Veredicto = 'MATCH' | 'RECHAZO' | 'SEGUNDA_CITA' | 'QUORUM_INSUFICIENTE';
export type EstadoCasamiento = 'VIGENTE' | 'RENOVADO' | 'DIVORCIADO';

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
  obligatorio: boolean;
  baseObligatoriedad: string | null;
  areas: string[];
}

export interface Ciclo {
  id: number;
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  estado: 'ABIERTO' | 'CERRADO';
}

export interface Transicion {
  procedimientoId: number;
  codigo: string;
  nombre: string;
  desde: EstadoProcedimiento;
  hacia: EstadoProcedimiento;
  mensaje: string;
}

export interface ResumenCierre {
  ciclo: Ciclo;
  resultadosCalculados: number;
  transiciones: Transicion[];
}

export interface Resultado {
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
  versionExpress: string | null;
  estado: EstadoProcedimiento;
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
  versionExpress: string | null;
  areaIds: number[];
}

export interface Casamiento {
  id: number;
  procedimientoId: number;
  codigo: string;
  nombre: string;
  obligatorio: boolean;
  versionExpress: string | null;
  fecha: string;
  votos: string;
  padrinoNombre: string | null;
  oficianteNombre: string;
  grupal: boolean;
  fechaAniversario: string;
  diasParaAniversario: number;
  estado: EstadoCasamiento;
  areas: string[];
}

export interface CasamientoRequest {
  procedimientoId: number;
  fecha: string | null;
  votos: string;
  versionExpress: string;
  oficianteId: number;
  padrinoId: number | null;
  areaIds: number[];
}

export interface Velorio {
  id: number;
  procedimientoId: number;
  codigo: string;
  nombre: string;
  fechaNacimiento: string;
  fecha: string;
  causaMuerte: string;
  epitafio: string;
  minutosSemanalesPerdidos: number;
  deudos: string;
  actaFirmada: boolean;
  firmadoPorNombre: string | null;
  fechaFirma: string | null;
}

export interface VelorioRequest {
  procedimientoId: number;
  fecha: string | null;
  causaMuerte: string;
  epitafio: string;
  deudos: string;
}

export interface SugerenciaVelorio {
  procedimientoId: number;
  codigo: string;
  nombre: string;
  causaMuerte: string;
  epitafio: string;
  minutosSemanalesPerdidos: number;
  deudos: string;
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
