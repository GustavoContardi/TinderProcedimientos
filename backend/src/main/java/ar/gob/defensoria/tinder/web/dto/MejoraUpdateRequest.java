package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.EstadoMejora;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Edición desde el tablero. RESUELTA y DESCARTADA exigen respuesta (422 si falta). */
public record MejoraUpdateRequest(
        @NotBlank(message = "Describí la mejora") String descripcion,
        @NotNull(message = "Falta el responsable") Long responsableId,
        @NotNull(message = "Falta la fecha de compromiso") LocalDate fechaCompromiso,
        @NotNull(message = "Falta el estado") EstadoMejora estado,
        String respuesta) {
}
