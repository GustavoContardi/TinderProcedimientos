package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findAllByOrderByRevisadaAscCreatedAtDesc();
}
