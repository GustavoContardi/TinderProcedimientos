package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.DesignacionJuez;

import java.time.LocalDate;

public record DesignacionDTO(Long id, Long usuarioId, String usuarioNombre, Long areaId, String areaNombre,
                             LocalDate desde, LocalDate hasta, boolean vigente) {

    public static DesignacionDTO de(DesignacionJuez d) {
        return new DesignacionDTO(d.getId(), d.getUsuario().getId(), d.getUsuario().getNombre(), d.getArea().getId(),
                d.getArea().getNombre(), d.getDesde(), d.getHasta(), d.isVigente());
    }
}
