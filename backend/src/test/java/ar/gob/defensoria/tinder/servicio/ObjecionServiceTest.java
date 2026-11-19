package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.EvaluarObjecionRequest;
import ar.gob.defensoria.tinder.web.dto.ObjecionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static ar.gob.defensoria.tinder.servicio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObjecionServiceTest {

    @Mock ObjecionRepository objeciones;
    @Mock ProcedimientoRepository procedimientos;
    @Mock CicloVotacionRepository ciclos;
    @Mock CasamientoRepository casamientos;
    @Mock MatrizParticipacionRepository matriz;
    @Mock SegundaCitaRepository citas;
    @Mock MejoraRepository mejoraRepo;
    @Mock UsuarioRepository usuarios;

    private ObjecionService service;
    private final Usuario juez = usuario(13L, "Mónica Calderón", Rol.JUEZ_PAZ, OPERACIONES);
    private final Usuario eva = usuario(17L, "Eva Romero", Rol.EMPLEADO, OPERACIONES);
    private final UUID id = UUID.fromString("a1000000-0000-4000-8000-000000000009");
    private Procedimiento f07;

    @BeforeEach
    void setUp() {
        service = new ObjecionService(objeciones, procedimientos, ciclos, casamientos, matriz, citas,
                new MejoraService(mejoraRepo, procedimientos, usuarios, props()), new MaquinaEstados(), props());
        f07 = procedimiento(7L, "F-07", false, EstadoProcedimiento.SEGUNDA_CITA, OPERACIONES);
        f07.setDueno(eva);
        lenient().when(procedimientos.findById(7L)).thenReturn(Optional.of(f07));
        lenient().when(mejoraRepo.siguienteNumero()).thenReturn(7L);
        lenient().when(mejoraRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Objecion objecion(OrigenObjecion origen) {
        Objecion o = new Objecion(7L, origen == OrigenObjecion.VOTO ? 3L : null, 1L, origen,
                CategoriaObjecion.DEMASIADOS_PASOS, "Son 5 preguntas, nadie las contesta");
        o.setId(id);
        when(objeciones.findById(id)).thenReturn(Optional.of(o));
        return o;
    }

    @Test
    @DisplayName("No fundada sin respuesta => 422: nadie queda sin respuesta")
    void noFundadaExigeRespuesta() {
        objecion(OrigenObjecion.VOTO);
        assertThatThrownBy(() -> service.evaluar(id, new EvaluarObjecionRequest(false, " ", null), autenticado(juez)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verifyNoInteractions(mejoraRepo);
    }

    @Test
    @DisplayName("Evaluar crea una mejora OBJECION para el dueño, a 30 días o la fecha indicada")
    void evaluarCreaMejora() {
        Objecion o = objecion(OrigenObjecion.VOTO);
        LocalDate fecha = LocalDate.now().plusDays(10);

        service.evaluar(id, new EvaluarObjecionRequest(true, null, fecha), autenticado(juez));

        assertThat(o.getEstado()).isEqualTo(EstadoObjecion.EVALUADA);
        assertThat(o.getFundada()).isTrue();
        ArgumentCaptor<Mejora> m = ArgumentCaptor.forClass(Mejora.class);
        verify(mejoraRepo).save(m.capture());
        assertThat(m.getValue().getOrigen()).isEqualTo(OrigenMejora.OBJECION);
        assertThat(m.getValue().getOrigenId()).isEqualTo(id.toString());
        assertThat(m.getValue().getResponsable()).isEqualTo(eva);
        assertThat(m.getValue().getFechaCompromiso()).isEqualTo(fecha);
    }

    @Test
    @DisplayName("Objeción de boda fundada => casamiento POSTERGADO y procedimiento a SEGUNDA_CITA")
    void bodaFundadaPosterga() {
        objecion(OrigenObjecion.BODA);
        f07.setEstado(EstadoProcedimiento.CASADO);
        Casamiento c = new Casamiento("L-012/2026", f07, LocalDate.now(), "votos", null, eva, juez, Set.of(OPERACIONES),
                Set.of(), List.of(), null, LocalDate.now().plusMonths(6));
        when(casamientos.findFirstByProcedimientoIdAndEstadoOrderByIdDesc(7L, EstadoCasamiento.VIGENTE)).thenReturn(Optional.of(c));

        service.evaluar(id, new EvaluarObjecionRequest(true, "Se agrega el interno de guardia", null), autenticado(juez));

        assertThat(c.getEstado()).isEqualTo(EstadoCasamiento.POSTERGADO);
        assertThat(f07.getEstado()).isEqualTo(EstadoProcedimiento.SEGUNDA_CITA);
        verify(citas).save(any());
    }

    @Test
    @DisplayName("Objetar una boda sin estar en la matriz => 403; estando, se guarda sin autor")
    void objetarBoda() {
        Casamiento c = new Casamiento("L-012/2026", f07, LocalDate.now(), "votos", null, eva, juez, Set.of(OPERACIONES),
                Set.of(), List.of(), null, LocalDate.now().plusMonths(6));
        when(casamientos.findById(2L)).thenReturn(Optional.of(c));
        var req = new ObjecionRequest(CategoriaObjecion.FALTA_ALGO, "Falta el interno de guardia");

        when(matriz.existsById(new MatrizParticipacion.Id(17L, 7L))).thenReturn(false);
        assertThatThrownBy(() -> service.objetarCasamiento(2L, req, autenticado(eva)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        when(matriz.existsById(new MatrizParticipacion.Id(17L, 7L))).thenReturn(true);
        assertThat(service.objetarCasamiento(2L, req, autenticado(eva)).origen()).isEqualTo(OrigenObjecion.BODA);
        verify(objeciones).save(argThat(o -> o.getOrigen() == OrigenObjecion.BODA && o.getCicloId() == null));
    }
}
