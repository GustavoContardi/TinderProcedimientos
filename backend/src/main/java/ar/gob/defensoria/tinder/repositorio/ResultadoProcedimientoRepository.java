package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.ResultadoProcedimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultadoProcedimientoRepository extends JpaRepository<ResultadoProcedimiento, Long> {

    List<ResultadoProcedimiento> findByCicloId(Long cicloId);

    Optional<ResultadoProcedimiento> findFirstByProcedimientoIdOrderByCicloFechaFinDescIdDesc(Long procedimientoId);
}
