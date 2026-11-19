package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.CicloVotacion;
import ar.gob.defensoria.tinder.dominio.EstadoCiclo;
import ar.gob.defensoria.tinder.dominio.TipoCiclo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CicloVotacionRepository extends JpaRepository<CicloVotacion, Long> {

    Optional<CicloVotacion> findFirstByEstadoAndTipo(EstadoCiclo estado, TipoCiclo tipo);

    Optional<CicloVotacion> findFirstByEstadoAndTipoOrderByFechaFinDesc(EstadoCiclo estado, TipoCiclo tipo);

    Optional<CicloVotacion> findFirstByTipoOrderByFechaInicioDesc(TipoCiclo tipo);

    List<CicloVotacion> findByEstadoAndTipo(EstadoCiclo estado, TipoCiclo tipo);

    boolean existsByProcedimientoIdAndEstadoAndTipo(Long procedimientoId, EstadoCiclo estado, TipoCiclo tipo);

    List<CicloVotacion> findAllByOrderByFechaInicioDescIdDesc();

    boolean existsByNombreIgnoreCase(String nombre);
}
