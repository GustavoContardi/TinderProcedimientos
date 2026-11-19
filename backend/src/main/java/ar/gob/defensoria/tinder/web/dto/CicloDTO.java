package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.CicloVotacion;
import ar.gob.defensoria.tinder.dominio.EstadoCiclo;
import ar.gob.defensoria.tinder.dominio.TipoCiclo;

import java.time.LocalDate;

public record CicloDTO(Long id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, EstadoCiclo estado,
                       TipoCiclo tipo, Long procedimientoId, String procedimientoCodigo) {

    public static CicloDTO de(CicloVotacion c) {
        var p = c.getProcedimiento();
        return new CicloDTO(c.getId(), c.getNombre(), c.getFechaInicio(), c.getFechaFin(), c.getEstado(), c.getTipo(),
                p != null ? p.getId() : null, p != null ? p.getCodigo() : null);
    }
}
