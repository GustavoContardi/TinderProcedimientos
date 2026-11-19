package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.MatrizRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MatrizRevisionRepository extends JpaRepository<MatrizRevision, Long> {

    Optional<MatrizRevision> findFirstByFechaBetweenOrderByFechaDesc(LocalDate desde, LocalDate hasta);

    Optional<MatrizRevision> findFirstByOrderByFechaDescIdDesc();
}
