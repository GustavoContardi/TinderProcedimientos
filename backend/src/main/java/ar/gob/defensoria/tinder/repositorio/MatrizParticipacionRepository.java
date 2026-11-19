package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.dominio.MatrizParticipacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MatrizParticipacionRepository extends JpaRepository<MatrizParticipacion, MatrizParticipacion.Id> {

    /** Cantidad de habilitados para votar un procedimiento. */
    long countByIdProcedimientoId(Long procedimientoId);

    List<MatrizParticipacion> findByIdProcedimientoId(Long procedimientoId);

    /** Áreas de los usuarios que participan del procedimiento. */
    @Query("""
            select distinct u.area.id from MatrizParticipacion m, Usuario u
            where u.id = m.id.usuarioId and m.id.procedimientoId = :procedimientoId
            """)
    List<Long> areasDelProcedimiento(@Param("procedimientoId") Long procedimientoId);

    /** Votos posibles en el trimestre: suma de la matriz de los procedimientos votables. */
    @Query("""
            select count(m) from MatrizParticipacion m, Procedimiento p
            where p.id = m.id.procedimientoId and p.estado in :estados
            """)
    long contarHabilitados(@Param("estados") Collection<EstadoProcedimiento> estados);

    @Modifying
    @Query("delete from MatrizParticipacion m where m.id.procedimientoId = :procedimientoId")
    void borrarDelProcedimiento(@Param("procedimientoId") Long procedimientoId);
}
