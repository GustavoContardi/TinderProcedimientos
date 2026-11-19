package ar.gob.defensoria.tinder.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Causa y epitafio son opcionales: si vienen vacíos se usan los sugeridos.
 * Reencarnación: reencarnacionId (existente), nuevoReemplazo (BORRADOR) o sinReemplazoMotivo.
 */
public record VelorioRequest(
        @NotNull(message = "Falta el procedimiento a velar") Long procedimientoId,
        LocalDate fecha,
        String causa,
        String epitafio,
        String deudos,
        String herencia,
        String lecciones,
        Long reencarnacionId,
        @Valid NuevoReemplazoRequest nuevoReemplazo,
        String sinReemplazoMotivo) {
}
