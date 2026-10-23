package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VotoRepository extends JpaRepository<Voto, UUID> {

    /** Conteo agregado por procedimiento, área y valor. Nunca se lee un voto individual. */
    @Query("""
            select new ar.gob.defensoria.tinder.repositorio.ConteoVotos(
                v.procedimientoId, v.areaId, v.valor, count(v))
            from Voto v where v.cicloId = :cicloId
            group by v.procedimientoId, v.areaId, v.valor
            """)
    List<ConteoVotos> contarPorCiclo(@Param("cicloId") Long cicloId);
}
