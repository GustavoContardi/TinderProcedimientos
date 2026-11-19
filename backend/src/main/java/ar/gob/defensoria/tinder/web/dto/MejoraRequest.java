package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.OrigenMejora;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Alta manual de una mejora (juez de paz). */
public record MejoraRequest(
        @NotNull(message = "Falta el procedimiento") Long procedimientoId,
        @NotNull(message = "Falta el origen") OrigenMejora origen,
        String origenId,
        @NotBlank(message = "Describí la mejora") String descripcion,
        Long responsableId,
        LocalDate fechaCompromiso) {
}
