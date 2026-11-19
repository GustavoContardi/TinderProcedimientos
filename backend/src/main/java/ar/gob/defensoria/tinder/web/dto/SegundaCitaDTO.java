package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.DecisionSegundaCita;
import ar.gob.defensoria.tinder.dominio.EstadoSegundaCita;

import java.time.LocalDate;

public record SegundaCitaDTO(
        Long id,
        ProcedimientoDTO procedimiento,
        Long cicloId,
        String cicloNombre,
        LocalDate fechaProgramada,
        EstadoSegundaCita estado,
        String minuta,
        DecisionSegundaCita decision,
        String condiciones,
        CicloDTO cicloRapido,
        Integer pctGlobal,
        Integer votantes,
        Integer habilitados,
        String motivo) {
}
