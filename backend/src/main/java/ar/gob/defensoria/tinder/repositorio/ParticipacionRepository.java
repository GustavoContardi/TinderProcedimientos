package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.Participacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipacionRepository extends JpaRepository<Participacion, Participacion.Id> {

    boolean existsByIdProcedimientoId(Long procedimientoId);
}
