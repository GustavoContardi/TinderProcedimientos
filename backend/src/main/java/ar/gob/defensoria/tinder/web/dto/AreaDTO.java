package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.Area;

public record AreaDTO(Long id, String nombre) {

    public static AreaDTO de(Area a) {
        return new AreaDTO(a.getId(), a.getNombre());
    }
}
