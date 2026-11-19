package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.dominio.Procedimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProcedimientoRepository extends JpaRepository<Procedimiento, Long> {

    List<Procedimiento> findByEstadoNotOrderByCodigo(EstadoProcedimiento estado);

    List<Procedimiento> findByEstadoOrderByCodigo(EstadoProcedimiento estado);

    List<Procedimiento> findByEstadoIn(Collection<EstadoProcedimiento> estados);

    List<Procedimiento> findByDuenoIdOrderByCodigo(Long duenoId);

    long countByEstado(EstadoProcedimiento estado);

    boolean existsByCodigoIgnoreCase(String codigo);

    /**
     * Mazo trimestral: procedimientos de la MATRIZ del usuario, en estados votables, que todavía no votó en el ciclo
     * y que no tienen una votación rápida abierta (esos se votan en su propio ciclo).
     */
    @Query("""
            select p from Procedimiento p
            where p.estado in :estados
              and exists (
                  select 1 from MatrizParticipacion m
                  where m.id.usuarioId = :usuarioId and m.id.procedimientoId = p.id)
              and not exists (
                  select 1 from VotoEmitido ve
                  where ve.id.usuarioId = :usuarioId
                    and ve.id.procedimientoId = p.id
                    and ve.id.cicloId = :cicloId)
              and not exists (
                  select 1 from CicloVotacion c
                  where c.procedimiento = p and c.estado = ar.gob.defensoria.tinder.dominio.EstadoCiclo.ABIERTO)
            """)
    List<Procedimiento> findMazo(@Param("usuarioId") Long usuarioId,
                                 @Param("cicloId") Long cicloId,
                                 @Param("estados") Collection<EstadoProcedimiento> estados);
}
