package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.Casamiento;
import ar.gob.defensoria.tinder.dominio.EstadoCasamiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CasamientoRepository extends JpaRepository<Casamiento, Long> {

    List<Casamiento> findAllByOrderByFechaDescIdDesc();

    List<Casamiento> findByEstadoAndFechaAniversarioLessThanEqualOrderByFechaAniversario(EstadoCasamiento estado, LocalDate hasta);
}
