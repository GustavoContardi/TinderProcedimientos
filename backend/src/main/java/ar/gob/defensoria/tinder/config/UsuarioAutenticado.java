package ar.gob.defensoria.tinder.config;

import ar.gob.defensoria.tinder.dominio.Rol;
import ar.gob.defensoria.tinder.dominio.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Principal de Spring Security con los datos mínimos del usuario logueado. */
public record UsuarioAutenticado(
        Long id,
        String nombre,
        String email,
        String passwordHash,
        Rol rol,
        Long areaId,
        String areaNombre) implements UserDetails {

    public static UsuarioAutenticado de(Usuario u) {
        return new UsuarioAutenticado(u.getId(), u.getNombre(), u.getEmail(), u.getPasswordHash(),
                u.getRol(), u.getArea().getId(), u.getArea().getNombre());
    }

    public boolean esEmpleado() {
        return rol == Rol.EMPLEADO;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String toString() {
        return "UsuarioAutenticado[" + email + ", " + rol + "]";
    }
}
