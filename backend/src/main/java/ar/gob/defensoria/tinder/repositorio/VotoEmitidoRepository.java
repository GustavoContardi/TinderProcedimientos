package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.VotoEmitido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoEmitidoRepository extends JpaRepository<VotoEmitido, VotoEmitido.Id> {

    boolean existsByIdProcedimientoId(Long procedimientoId);

    long countByIdCicloId(Long cicloId);
}
