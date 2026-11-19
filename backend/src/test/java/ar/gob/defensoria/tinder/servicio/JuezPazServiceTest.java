package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.DesignacionJuez;
import ar.gob.defensoria.tinder.dominio.Rol;
import ar.gob.defensoria.tinder.dominio.Usuario;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.DesignacionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static ar.gob.defensoria.tinder.servicio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JuezPazServiceTest {

    @Mock DesignacionJuezRepository designaciones;
    @Mock UsuarioRepository usuarios;
    @Mock AreaRepository areas;
    @Mock CicloVotacionRepository ciclos;
    @Mock MatrizRevisionRepository revisiones;
    @Mock MatrizParticipacionRepository matriz;
    @Mock VotoEmitidoRepository votosEmitidos;
    @Mock ResultadoProcedimientoRepository resultadosProc;
    @Mock SegundaCitaRepository citas;
    @Mock ProcedimientoRepository procedimientos;
    @Mock VelorioRepository velorios;
    @Mock MejoraRepository mejoras;
    @InjectMocks JuezPazService service;

    private final Usuario monica = usuario(13L, "Mónica Calderón", Rol.JUEZ_PAZ, OPERACIONES);
    private final Usuario carla = usuario(3L, "Carla Medina", Rol.EMPLEADO, OPERACIONES);

    @BeforeEach
    void setUp() {
        lenient().when(areas.findById(1L)).thenReturn(Optional.of(OPERACIONES));
        lenient().when(usuarios.findById(3L)).thenReturn(Optional.of(carla));
        lenient().when(usuarios.findById(13L)).thenReturn(Optional.of(monica));
        lenient().when(designaciones.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Designar un nuevo juez cierra la designación vigente y el anterior vuelve a EMPLEADO")
    void nuevoJuez() {
        DesignacionJuez vigente = new DesignacionJuez(monica, OPERACIONES, LocalDate.of(2026, 1, 1));
        when(designaciones.findFirstByAreaIdAndHastaIsNull(1L)).thenReturn(Optional.of(vigente));

        var dto = service.designar(new DesignacionRequest(3L, 1L));

        assertThat(monica.getRol()).isEqualTo(Rol.EMPLEADO);
        assertThat(carla.getRol()).isEqualTo(Rol.JUEZ_PAZ);
        assertThat(vigente.getHasta()).isEqualTo(LocalDate.now());
        assertThat(dto.vigente()).isTrue();
        assertThat(dto.usuarioNombre()).isEqualTo("Carla Medina");
        verify(designaciones).saveAndFlush(vigente);
    }

    @Test
    @DisplayName("El juez de paz tiene que ser del área => 422")
    void deOtraArea() {
        Usuario elena = usuario(5L, "Elena Quiroga", Rol.EMPLEADO, DESARROLLO);
        when(usuarios.findById(5L)).thenReturn(Optional.of(elena));
        assertThatThrownBy(() -> service.designar(new DesignacionRequest(5L, 1L))).isInstanceOf(ApiException.class);
        assertThat(elena.getRol()).isEqualTo(Rol.EMPLEADO);
    }
}
