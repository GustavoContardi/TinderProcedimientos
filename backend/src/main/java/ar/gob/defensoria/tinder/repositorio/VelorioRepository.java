package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.Velorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VelorioRepository extends JpaRepository<Velorio, Long> {

    List<Velorio> findAllByOrderByFechaDescIdDesc();

    boolean existsByProcedimientoId(Long procedimientoId);
}
