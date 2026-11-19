package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.DesignacionJuez;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignacionJuezRepository extends JpaRepository<DesignacionJuez, Long> {

    Optional<DesignacionJuez> findFirstByAreaIdAndHastaIsNull(Long areaId);

    boolean existsByUsuarioIdAndHastaIsNull(Long usuarioId);

    List<DesignacionJuez> findAllByOrderByDesdeDescIdDesc();
}
