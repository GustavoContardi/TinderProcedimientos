package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.EstadoObjecion;
import ar.gob.defensoria.tinder.dominio.Objecion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObjecionRepository extends JpaRepository<Objecion, UUID> {

    List<Objecion> findByProcedimientoId(Long procedimientoId);

    long countByProcedimientoId(Long procedimientoId);

    long countByProcedimientoIdAndEstadoNot(Long procedimientoId, EstadoObjecion estado);

    /** Objeción fundada sin respuesta: bloquea el casamiento. */
    boolean existsByProcedimientoIdAndFundadaTrueAndRespuestaIsNull(Long procedimientoId);
}
