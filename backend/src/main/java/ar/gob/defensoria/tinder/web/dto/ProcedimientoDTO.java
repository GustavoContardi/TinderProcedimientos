package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.dominio.TipoProcedimiento;

import java.time.LocalDate;
import java.util.List;

public record ProcedimientoDTO(
        Long id,
        String codigo,
        String nombre,
        TipoProcedimiento tipo,
        String bio,
        LocalDate fechaCreacion,
        int edadAnios,
        int minutosPorUso,
        int usosPorSemana,
        int minutosSemanales,
        String beneficio,
        boolean obligatorio,
        String baseObligatoriedad,
        Long autorId,
        String padrinoNombre,
        String versionExpress,
        EstadoProcedimiento estado,
        List<AreaDTO> areas) {
}
