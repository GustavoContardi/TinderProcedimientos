package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.ResultadoCiclo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResultadoCicloRepository extends JpaRepository<ResultadoCiclo, Long> {

    List<ResultadoCiclo> findByCicloIdOrderByAreaNombreAscProcedimientoCodigoAsc(Long cicloId);

    /** Resultados del último ciclo cerrado en el que se evaluó el procedimiento. */
    @Query("""
            select r from ResultadoCiclo r
            where r.procedimiento.id = :procedimientoId
              and r.ciclo.fechaFin = (select max(r2.ciclo.fechaFin) from ResultadoCiclo r2
                                      where r2.procedimiento.id = :procedimientoId)
            """)
    List<ResultadoCiclo> ultimosDelProcedimiento(@Param("procedimientoId") Long procedimientoId);

    Optional<ResultadoCiclo> findFirstByProcedimientoIdAndAreaIdOrderByCicloFechaFinDesc(Long procedimientoId, Long areaId);

    boolean existsByProcedimientoId(Long procedimientoId);
}
