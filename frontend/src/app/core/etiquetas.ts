import { EstadoProcedimiento, Veredicto } from './models';

export const ESTADOS: Record<EstadoProcedimiento, { texto: string; icono: string; clase: string }> = {
  EN_TINDER: { texto: 'En Tinder', icono: '🔥', clase: 'estado-tinder' },
  SEGUNDA_CITA: { texto: 'Segunda cita', icono: '☕', clase: 'estado-segunda' },
  PENDIENTE_CASAMIENTO: { texto: 'Pendiente de casamiento', icono: '💌', clase: 'estado-pend-casamiento' },
  CASADO: { texto: 'Casado', icono: '💍', clase: 'estado-casado' },
  PENDIENTE_VELORIO: { texto: 'Pendiente de velorio', icono: '🕯️', clase: 'estado-pend-velorio' },
  VELADO: { texto: 'Velado', icono: '⚰️', clase: 'estado-velado' },
};

export const VEREDICTOS: Record<Veredicto, { texto: string; clase: string }> = {
  MATCH: { texto: 'Match', clase: 'veredicto-match' },
  RECHAZO: { texto: 'Rechazo', clase: 'veredicto-rechazo' },
  SEGUNDA_CITA: { texto: 'Segunda cita', clase: 'veredicto-segunda' },
  QUORUM_INSUFICIENTE: { texto: 'Pocos votos', clase: 'veredicto-quorum' },
};

export const ESTADOS_LISTA = Object.keys(ESTADOS) as EstadoProcedimiento[];

export function hoyIso(): string {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
}
