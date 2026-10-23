package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.CicloVotacion;
import ar.gob.defensoria.tinder.dominio.EstadoCiclo;

import java.time.LocalDate;

public record CicloDTO(Long id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, EstadoCiclo estado) {

    public static CicloDTO de(CicloVotacion c) {
        return new CicloDTO(c.getId(), c.getNombre(), c.getFechaInicio(), c.getFechaFin(), c.getEstado());
    }
}
