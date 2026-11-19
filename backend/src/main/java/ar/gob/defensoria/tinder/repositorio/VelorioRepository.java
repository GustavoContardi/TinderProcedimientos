package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.Velorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VelorioRepository extends JpaRepository<Velorio, Long> {

    List<Velorio> findAllByOrderByFechaDescIdDesc();

    boolean existsByProcedimientoId(Long procedimientoId);

    Optional<Velorio> findByProcedimientoId(Long procedimientoId);

    @Query("select count(v) from Velorio v where v.firmaJuezPor is null or v.firmaDireccionPor is null or v.firmaDuenoPor is null")
    long contarSinFirmasCompletas();

    /** Mayor número de acta del año ("004/2026" → 4). */
    @Query(value = """
            select coalesce(max(cast(split_part(numero_acta, '/', 1) as integer)), 0)
            from velorio where numero_acta like concat('%/', :anio)
            """, nativeQuery = true)
    int ultimoNumeroDelAnio(@Param("anio") String anio);
}
