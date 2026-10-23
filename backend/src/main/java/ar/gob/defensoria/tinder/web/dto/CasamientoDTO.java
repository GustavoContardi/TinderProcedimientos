package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.EstadoCasamiento;

import java.time.LocalDate;
import java.util.List;

public record CasamientoDTO(
        Long id,
        Long procedimientoId,
        String codigo,
        String nombre,
        boolean obligatorio,
        String versionExpress,
        LocalDate fecha,
        String votos,
        String padrinoNombre,
        String oficianteNombre,
        boolean grupal,
        LocalDate fechaAniversario,
        long diasParaAniversario,
        EstadoCasamiento estado,
        List<String> areas) {
}
