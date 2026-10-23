package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.Rol;
import ar.gob.defensoria.tinder.dominio.Usuario;

public record UsuarioDTO(Long id, String nombre, String email, Rol rol, Long areaId, String areaNombre) {

    public static UsuarioDTO de(UsuarioAutenticado u) {
        return new UsuarioDTO(u.id(), u.nombre(), u.email(), u.rol(), u.areaId(), u.areaNombre());
    }

    public static UsuarioDTO de(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getNombre(), u.getEmail(), u.getRol(),
                u.getArea().getId(), u.getArea().getNombre());
    }
}
