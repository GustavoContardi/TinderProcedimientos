package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.CasamientoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ar.gob.defensoria.tinder.servicio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasamientoServiceTest {

    @Mock CasamientoRepository casamientos;
    @Mock ProcedimientoRepository procedimientos;
    @Mock UsuarioRepository usuarios;
    @Mock ObjecionRepository objeciones;
    @Mock ResultadoCicloRepository resultados;
    @Mock SegundaCitaRepository citas;
    @Mock MejoraRepository mejoraRepo;

    private CasamientoService service;
    private Procedimiento pr03;
    private final Usuario juez = usuario(13L, "Mónica Calderón", Rol.JUEZ_PAZ, OPERACIONES);
    private final Usuario direccion = usuario(14L, "Ricardo Ferreyra", Rol.DIRECCION, DESARROLLO);
    private final Usuario rocio = usuario(19L, "Rocío Almada", Rol.EMPLEADO, ATENCION);
    private final Usuario ana = usuario(1L, "Ana Paredes", Rol.EMPLEADO, OPERACIONES);

    @BeforeEach
    void setUp() {
        service = new CasamientoService(casamientos, procedimientos, usuarios, objeciones, resultados, citas,
                new MejoraService(mejoraRepo, procedimientos, usuarios, props()), new MaquinaEstados(), props());
        pr03 = procedimiento(3L, "PR-03", false, EstadoProcedimiento.PENDIENTE_CASAMIENTO, OPERACIONES, DESARROLLO, INFRA);
        pr03.setAutor(ana);
        lenient().when(procedimientos.findById(3L)).thenReturn(Optional.of(pr03));
        lenient().when(usuarios.findById(13L)).thenReturn(Optional.of(juez));
        lenient().when(usuarios.findById(14L)).thenReturn(Optional.of(direccion));
        lenient().when(usuarios.findById(19L)).thenReturn(Optional.of(rocio));
        lenient().when(usuarios.findById(1L)).thenReturn(Optional.of(ana));
    }

    private static CasamientoRequest req(Long oficianteId, Set<Long> testigos) {
        return new CasamientoRequest(3L, LocalDate.of(2026, 11, 19), "Prometemos usarte", "1. Llamá a la mesa",
                oficianteId, null, null, testigos, null, null);
    }

    private static void es422(Throwable e, String codigo) {
        ApiException api = (ApiException) e;
        assertThat(api.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(api.getCodigo()).isEqualTo(codigo);
    }

    @Test
    @DisplayName("Casamiento con una objeción fundada sin respuesta => 422")
    void objecionFundadaSinRespuesta() {
        when(objeciones.existsByProcedimientoIdAndFundadaTrueAndRespuestaIsNull(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.casar(req(13L, Set.of())))
                .isInstanceOf(ApiException.class).satisfies(e -> es422(e, "OBJECION_SIN_RESPUESTA"));
        assertThat(pr03.getEstado()).isEqualTo(EstadoProcedimiento.PENDIENTE_CASAMIENTO);
        verify(casamientos, never()).save(any());
    }

    @Test
    @DisplayName("Oficiante que no es JUEZ_PAZ => 422")
    void oficianteNoEsJuez() {
        assertThatThrownBy(() -> service.casar(req(14L, Set.of())))
                .isInstanceOf(ApiException.class).satisfies(e -> es422(e, "OFICIANTE_INVALIDO"));
        verify(casamientos, never()).save(any());
    }

    @Test
    @DisplayName("Los testigos tienen que ser de OTRAS áreas => 422 si es de un área que se casa")
    void testigoDeUnAreaQueSeCasa() {
        assertThatThrownBy(() -> service.casar(req(13L, Set.of(1L))))
                .isInstanceOf(ApiException.class).satisfies(e -> es422(e, "TESTIGO_INVALIDO"));
    }

    @Test
    @DisplayName("Casamiento grupal: número de libreta, foto por área, padrino = autor, condiciones de la segunda cita")
    void casamientoOk() {
        when(casamientos.ultimoNumeroDelAnio("2026")).thenReturn(11);
        when(resultados.ultimosDelProcedimiento(3L)).thenReturn(List.of(
                new ResultadoCiclo(null, pr03, OPERACIONES, 6, 1, new BigDecimal("0.8571"), Veredicto.CASAMIENTO),
                new ResultadoCiclo(null, pr03, INFRA, 1, 1, new BigDecimal("0.5000"), Veredicto.QUORUM_INSUFICIENTE)));
        SegundaCita cita = new SegundaCita(pr03, null, LocalDate.now());
        cita.realizar("minuta", DecisionSegundaCita.CASAR_CON_CONDICIONES, "Revisar en 3 meses");
        when(citas.findFirstByProcedimientoIdAndDecisionOrderByIdDesc(3L, DecisionSegundaCita.CASAR_CON_CONDICIONES))
                .thenReturn(Optional.of(cita));
        when(casamientos.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = service.casar(req(13L, Set.of(19L)));

        assertThat(dto.numero()).isEqualTo("L-012/2026");
        assertThat(dto.grupal()).isTrue();
        assertThat(dto.padrinoNombre()).isEqualTo("Ana Paredes");
        assertThat(dto.oficianteNombre()).isEqualTo("Mónica Calderón");
        assertThat(dto.testigos()).extracting(t -> t.nombre()).containsExactly("Rocío Almada");
        assertThat(dto.resultados()).extracting(r -> r.pct()).containsExactly(86);
        assertThat(dto.condiciones()).isEqualTo("Revisar en 3 meses");
        assertThat(dto.fechaAniversario()).isEqualTo(LocalDate.of(2027, 5, 19));
        assertThat(pr03.getEstado()).isEqualTo(EstadoProcedimiento.CASADO);
        verify(casamientos).ultimoNumeroDelAnio(anyString());
    }
}
