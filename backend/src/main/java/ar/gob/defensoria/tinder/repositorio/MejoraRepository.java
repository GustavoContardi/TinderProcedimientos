package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.EstadoMejora;
import ar.gob.defensoria.tinder.dominio.Mejora;
import ar.gob.defensoria.tinder.dominio.OrigenMejora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MejoraRepository extends JpaRepository<Mejora, Long> {

    List<Mejora> findAllByOrderByFechaCompromisoAscIdAsc();

    List<Mejora> findByProcedimientoIdAndEstadoNotIn(Long procedimientoId, Collection<EstadoMejora> estados);

    Optional<Mejora> findFirstByOrigenAndOrigenId(OrigenMejora origen, String origenId);

    long countByEstadoNotInAndFechaCompromisoBefore(Collection<EstadoMejora> estados, LocalDate fecha);

    /** Número para el código M-001, M-002… (secuencia propia, no depende del id). */
    @Query(value = "select nextval('mejora_codigo_seq')", nativeQuery = true)
    long siguienteNumero();
}
