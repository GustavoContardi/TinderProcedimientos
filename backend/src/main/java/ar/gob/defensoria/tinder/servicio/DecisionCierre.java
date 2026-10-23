package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.dominio.TipoAlerta;

/**
 * Resultado de consolidar los veredictos de todas las áreas de un procedimiento al cerrar un ciclo.
 *
 * @param destino estado al que pasa el procedimiento
 * @param alerta  alerta para Calidad (puede ser null)
 * @param mensaje explicación legible de la decisión
 */
public record DecisionCierre(EstadoProcedimiento destino, TipoAlerta alerta, String mensaje) {
}
