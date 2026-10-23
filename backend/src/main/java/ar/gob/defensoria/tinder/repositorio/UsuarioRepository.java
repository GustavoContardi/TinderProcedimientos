package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.Rol;
import ar.gob.defensoria.tinder.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findByRolOrderByNombre(Rol rol);

    List<Usuario> findAllByOrderByNombre();
}
