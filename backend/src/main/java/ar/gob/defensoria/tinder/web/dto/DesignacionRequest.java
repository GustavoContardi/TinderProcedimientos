package ar.gob.defensoria.tinder.web.dto;

import jakarta.validation.constraints.NotNull;

public record DesignacionRequest(
        @NotNull(message = "Elegí a quién designar") Long usuarioId,
        @NotNull(message = "Elegí el área") Long areaId) {
}
