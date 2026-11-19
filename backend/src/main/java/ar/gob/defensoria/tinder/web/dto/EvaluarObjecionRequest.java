package ar.gob.defensoria.tinder.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** fundada sin respuesta queda EVALUADA (bloquea el casamiento); no fundada exige respuesta. */
public record EvaluarObjecionRequest(
        @NotNull(message = "Indicá si la objeción es fundada o no") Boolean fundada,
        String respuesta,
        LocalDate fechaCompromiso) {
}
