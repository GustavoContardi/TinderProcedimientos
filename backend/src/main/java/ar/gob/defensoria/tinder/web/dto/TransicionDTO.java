package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;

public record TransicionDTO(Long procedimientoId, String codigo, String nombre,
                            EstadoProcedimiento desde, EstadoProcedimiento hacia, String mensaje) {
}
