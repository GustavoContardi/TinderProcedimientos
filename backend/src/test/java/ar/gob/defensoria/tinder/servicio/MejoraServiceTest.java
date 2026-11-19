package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.MejoraRepository;
import ar.gob.defensoria.tinder.repositorio.ProcedimientoRepository;
import ar.gob.defensoria.tinder.repositorio.UsuarioRepository;
import ar.gob.defensoria.tinder.web.dto.MejoraUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static ar.gob.defensoria.tinder.servicio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MejoraServiceTest {

    @Mock MejoraRepository mejoras;
    @Mock ProcedimientoRepository procedimientos;
    @Mock UsuarioRepository usuarios;

    private MejoraService service;
    private Mejora m;
    private final Usuario julian = usuario(10L, "Julián Ortega", Rol.EMPLEADO, INFRA);
    private final Usuario juez = usuario(13L, "Mónica Calderón", Rol.JUEZ_PAZ, OPERACIONES);
    private final Usuario ana = usuario(1L, "Ana Paredes", Rol.EMPLEADO, OPERACIONES);

    @BeforeEach
    void setUp() {
        service = new MejoraService(mejoras, procedimientos, usuarios, props());
        Procedimiento pr02 = procedimiento(2L, "PR-02", true, EstadoProcedimiento.SEGUNDA_CITA, INFRA);
        m = new Mejora("M-005", OrigenMejora.REFORMULACION, "2", pr02, "Reformular PR-02", julian, LocalDate.now().plusDays(20));
        m.setId(5L);
        when(mejoras.findById(5L)).thenReturn(Optional.of(m));
    }

    private static MejoraUpdateRequest req(EstadoMejora estado, String respuesta) {
        return new MejoraUpdateRequest("Reformular PR-02", 10L, LocalDate.now().plusDays(20), estado, respuesta);
    }

    @Test
    @DisplayName("Pasar una mejora a DESCARTADA sin respuesta => 422")
    void descartadaSinRespuesta() {
        assertThatThrownBy(() -> service.actualizar(5L, req(EstadoMejora.DESCARTADA, "  "), autenticado(juez)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(((ApiException) e).getCodigo()).isEqualTo("RESPUESTA_OBLIGATORIA");
                });
        assertThat(m.getEstado()).isEqualTo(EstadoMejora.NUEVA);
    }

    @Test
    @DisplayName("El responsable puede moverla y cerrarla con respuesta")
    void responsableLaResuelve() {
        var dto = service.actualizar(5L, req(EstadoMejora.RESUELTA, "Se armó la versión 3"), autenticado(julian));
        assertThat(dto.estado()).isEqualTo(EstadoMejora.RESUELTA);
        assertThat(dto.respuesta()).isEqualTo("Se armó la versión 3");
    }

    @Test
    @DisplayName("Quien no es juez ni responsable no la edita => 403")
    void otroNoEdita() {
        assertThatThrownBy(() -> service.actualizar(5L, req(EstadoMejora.EN_CURSO, null), autenticado(ana)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
