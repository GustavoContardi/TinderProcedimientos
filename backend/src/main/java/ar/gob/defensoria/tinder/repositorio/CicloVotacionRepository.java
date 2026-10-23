package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.CicloVotacion;
import ar.gob.defensoria.tinder.dominio.EstadoCiclo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CicloVotacionRepository extends JpaRepository<CicloVotacion, Long> {

    Optional<CicloVotacion> findFirstByEstado(EstadoCiclo estado);

    Optional<CicloVotacion> findFirstByEstadoOrderByFechaFinDesc(EstadoCiclo estado);

    List<CicloVotacion> findAllByOrderByFechaInicioDesc();

    boolean existsByNombreIgnoreCase(String nombre);
}
