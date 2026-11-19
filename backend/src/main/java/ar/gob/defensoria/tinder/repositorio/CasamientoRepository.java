package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.Casamiento;
import ar.gob.defensoria.tinder.dominio.EstadoCasamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CasamientoRepository extends JpaRepository<Casamiento, Long> {

    List<Casamiento> findAllByOrderByFechaDescIdDesc();

    List<Casamiento> findByEstadoAndFechaAniversarioLessThanEqualOrderByFechaAniversario(EstadoCasamiento estado, LocalDate hasta);

    Optional<Casamiento> findFirstByProcedimientoIdAndEstadoOrderByIdDesc(Long procedimientoId, EstadoCasamiento estado);

    /** Mayor número de libreta del año ("L-012/2026" → 12). */
    @Query(value = """
            select coalesce(max(cast(split_part(substring(numero from 3), '/', 1) as integer)), 0)
            from casamiento where numero like concat('L-%/', :anio)
            """, nativeQuery = true)
    int ultimoNumeroDelAnio(@Param("anio") String anio);
}
