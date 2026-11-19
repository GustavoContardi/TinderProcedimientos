package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.DecisionSegundaCita;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Minuta y decisión de la segunda cita. lanzarRapida solo se tiene en cuenta con AJUSTAR_Y_REVOTAR
 * (las otras decisiones siempre abren una votación rápida).
 */
public record RealizarCitaRequest(
        @NotBlank(message = "Sin minuta no hubo reunión") String minuta,
        @NotNull(message = "Falta la decisión") DecisionSegundaCita decision,
        String condiciones,
        boolean lanzarRapida) {
}
