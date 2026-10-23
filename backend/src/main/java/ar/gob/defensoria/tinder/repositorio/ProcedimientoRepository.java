package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.dominio.Procedimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProcedimientoRepository extends JpaRepository<Procedimiento, Long> {

    List<Procedimiento> findAllByOrderByCodigo();

    List<Procedimiento> findByEstadoOrderByCodigo(EstadoProcedimiento estado);

    List<Procedimiento> findByEstadoIn(Collection<EstadoProcedimiento> estados);

    boolean existsByCodigoIgnoreCase(String codigo);

    /** Mazo: procedimientos del área, en estados votables, que el usuario todavía no votó en el ciclo. */
    @Query("""
            select distinct p from Procedimiento p join p.areas a
            where a.id = :areaId
              and p.estado in :estados
              and not exists (
                  select 1 from Participacion pa
                  where pa.id.usuarioId = :usuarioId
                    and pa.id.procedimientoId = p.id
                    and pa.id.cicloId = :cicloId)
            """)
    List<Procedimiento> findMazo(@Param("areaId") Long areaId,
                                 @Param("usuarioId") Long usuarioId,
                                 @Param("cicloId") Long cicloId,
                                 @Param("estados") Collection<EstadoProcedimiento> estados);
}
