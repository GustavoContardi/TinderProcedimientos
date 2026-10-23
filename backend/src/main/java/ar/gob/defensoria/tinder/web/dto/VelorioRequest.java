package ar.gob.defensoria.tinder.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Los campos de texto son opcionales: si vienen vacíos se usan los sugeridos. */
public record VelorioRequest(
        @NotNull(message = "Falta el procedimiento a velar") Long procedimientoId,
        LocalDate fecha,
        String causaMuerte,
        String epitafio,
        String deudos) {
}
