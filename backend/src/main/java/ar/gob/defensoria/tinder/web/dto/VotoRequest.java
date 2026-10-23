package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.ValorVoto;
import jakarta.validation.constraints.NotNull;

public record VotoRequest(
        @NotNull(message = "Falta el procedimiento") Long procedimientoId,
        @NotNull(message = "Falta el voto: LIKE o NOPE") ValorVoto valor) {
}
