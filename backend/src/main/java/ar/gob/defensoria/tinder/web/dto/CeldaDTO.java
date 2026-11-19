package ar.gob.defensoria.tinder.web.dto;

import jakarta.validation.constraints.NotNull;

public record CeldaDTO(@NotNull Long usuarioId, @NotNull Long procedimientoId) {
}
