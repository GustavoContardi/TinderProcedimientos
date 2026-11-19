package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.dominio.Veredicto;

public record TransicionDTO(Long procedimientoId, String codigo, String nombre, Veredicto destino,
                            EstadoProcedimiento desde, EstadoProcedimiento hacia, String mensaje) {
}
