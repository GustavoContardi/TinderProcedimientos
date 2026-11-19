package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.EstadoMejora;
import ar.gob.defensoria.tinder.dominio.OrigenMejora;

import java.time.LocalDate;

public record MejoraDTO(
        Long id,
        String codigo,
        OrigenMejora origen,
        String origenId,
        Long procedimientoId,
        String procedimientoCodigo,
        String procedimientoNombre,
        String descripcion,
        Long responsableId,
        String responsableNombre,
        LocalDate fechaCompromiso,
        boolean vencida,
        EstadoMejora estado,
        String respuesta) {
}
