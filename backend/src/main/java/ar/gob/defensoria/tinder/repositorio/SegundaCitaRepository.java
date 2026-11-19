package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.DecisionSegundaCita;
import ar.gob.defensoria.tinder.dominio.EstadoSegundaCita;
import ar.gob.defensoria.tinder.dominio.SegundaCita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SegundaCitaRepository extends JpaRepository<SegundaCita, Long> {

    List<SegundaCita> findAllByOrderByEstadoAscFechaProgramadaAscIdAsc();

    List<SegundaCita> findByEstadoOrderByFechaProgramadaAscIdAsc(EstadoSegundaCita estado);

    Optional<SegundaCita> findFirstByCicloRapidoId(Long cicloId);

    Optional<SegundaCita> findFirstByProcedimientoIdAndEstadoOrderByIdDesc(Long procedimientoId, EstadoSegundaCita estado);

    Optional<SegundaCita> findFirstByProcedimientoIdAndDecisionOrderByIdDesc(Long procedimientoId, DecisionSegundaCita decision);

    boolean existsByCicloIdAndProcedimientoId(Long cicloId, Long procedimientoId);
}
