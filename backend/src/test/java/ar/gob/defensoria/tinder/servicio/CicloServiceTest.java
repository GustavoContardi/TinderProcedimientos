package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static ar.gob.defensoria.tinder.servicio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CicloServiceTest {

    @Mock CicloVotacionRepository ciclos;
    @Mock ProcedimientoRepository procedimientos;
    @Mock VotoRepository votos;
    @Mock ResultadoCicloRepository resultados;
    @Mock ResultadoProcedimientoRepository resultadosProc;
    @Mock MatrizParticipacionRepository matriz;
    @Mock AreaRepository areas;
    @Mock AlertaRepository alertas;
    @Mock SegundaCitaRepository citas;
    @Mock MejoraRepository mejoraRepo;
    @Mock UsuarioRepository usuarios;

    private CicloService service;
    private CicloVotacion ciclo;
    private final Usuario julian = usuario(10L, "Julián Ortega", Rol.EMPLEADO, INFRA);

    @BeforeEach
    void setUp() {
        var mejoras = new MejoraService(mejoraRepo, procedimientos, usuarios, props());
        service = new CicloService(ciclos, procedimientos, votos, resultados, resultadosProc, matriz, areas, alertas,
                citas, mejoras, new ClasificadorVeredicto(props()), new MaquinaEstados(), props());
        ciclo = new CicloVotacion("2026-T4", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31));
        ciclo.setId(4L);
        lenient().when(ciclos.findById(4L)).thenReturn(Optional.of(ciclo));
        lenient().when(areas.findAll()).thenReturn(List.of(OPERACIONES, DESARROLLO, INFRA));
        lenient().when(ciclos.findByEstadoAndTipo(EstadoCiclo.ABIERTO, TipoCiclo.RAPIDA)).thenReturn(List.of());
        lenient().when(mejoraRepo.siguienteNumero()).thenReturn(7L);
        lenient().when(mejoraRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private void enJuego(Procedimiento p, int habilitados, List<Long> areasMatriz, ConteoVotos... conteos) {
        when(procedimientos.findByEstadoIn(any())).thenReturn(List.of(p));
        when(votos.contarPorCiclo(4L)).thenReturn(List.of(conteos));
        when(matriz.areasDelProcedimiento(p.getId())).thenReturn(areasMatriz);
        when(matriz.countByIdProcedimientoId(p.getId())).thenReturn((long) habilitados);
    }

    private ResultadoProcedimiento resultadoGuardado() {
        ArgumentCaptor<ResultadoProcedimiento> r = ArgumentCaptor.forClass(ResultadoProcedimiento.class);
        verify(resultadosProc).save(r.capture());
        return r.getValue();
    }

    @Test
    @DisplayName("Participación del 40 % de la matriz => PARTICIPACION_INSUFICIENTE y el estado no cambia")
    void participacionInsuficienteNoCambiaEstado() {
        Procedimiento pr01 = procedimiento(1L, "PR-01", false, EstadoProcedimiento.EN_TINDER, DESARROLLO);
        pr01.setSegundasCitasConsecutivas(1);
        enJuego(pr01, 10, List.of(2L), new ConteoVotos(1L, 2L, ValorVoto.LIKE, 4));

        var resumen = service.cerrar(4L, 13L);

        assertThat(pr01.getEstado()).isEqualTo(EstadoProcedimiento.EN_TINDER);
        assertThat(resultadoGuardado().getDestino()).isEqualTo(Veredicto.PARTICIPACION_INSUFICIENTE);
        assertThat(resumen.transiciones()).singleElement()
                .satisfies(t -> assertThat(t.destino()).isEqualTo(Veredicto.PARTICIPACION_INSUFICIENTE));
        verify(citas, never()).save(any());
        assertThat(ciclo.getEstado()).isEqualTo(EstadoCiclo.CERRADO);
    }

    @Test
    @DisplayName("Un obligatorio con 10 % va a SEGUNDA_CITA (con cita convocada), nunca a PENDIENTE_VELORIO")
    void obligatorioCon10PorCiento() {
        Procedimiento pr02 = procedimiento(2L, "PR-02", true, EstadoProcedimiento.EN_TINDER, DESARROLLO);
        enJuego(pr02, 10, List.of(2L), new ConteoVotos(2L, 2L, ValorVoto.LIKE, 1), new ConteoVotos(2L, 2L, ValorVoto.NOPE, 9));

        service.cerrar(4L, 13L);

        assertThat(pr02.getEstado()).isEqualTo(EstadoProcedimiento.SEGUNDA_CITA);
        assertThat(resultadoGuardado().getMensaje()).startsWith("Obligatorio por norma: se simplifica, no se vela");
        verify(citas).save(argThat(c -> c.getProcedimiento() == pr02 && c.getEstado() == EstadoSegundaCita.CONVOCADA));
        verify(alertas).save(argThat(a -> a.getTipo() == TipoAlerta.OBLIGATORIO_NO_VELABLE));
    }

    @Test
    @DisplayName("Dos segundas citas consecutivas => requiere_reformulacion y una mejora REFORMULACION para el dueño")
    void dosSegundasCitasPidenReformulacion() {
        Procedimiento f07 = procedimiento(7L, "F-07", false, EstadoProcedimiento.SEGUNDA_CITA, OPERACIONES);
        f07.setDueno(julian);
        f07.setSegundasCitasConsecutivas(1);
        enJuego(f07, 10, List.of(1L), new ConteoVotos(7L, 1L, ValorVoto.LIKE, 5), new ConteoVotos(7L, 1L, ValorVoto.NOPE, 5));

        service.cerrar(4L, 13L);

        assertThat(f07.getSegundasCitasConsecutivas()).isEqualTo(2);
        assertThat(f07.isRequiereReformulacion()).isTrue();
        ArgumentCaptor<Mejora> m = ArgumentCaptor.forClass(Mejora.class);
        verify(mejoraRepo).save(m.capture());
        assertThat(m.getValue().getOrigen()).isEqualTo(OrigenMejora.REFORMULACION);
        assertThat(m.getValue().getResponsable()).isEqualTo(julian);
        assertThat(m.getValue().getCodigo()).isEqualTo("M-007");
        assertThat(m.getValue().getFechaCompromiso()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    @DisplayName("Con cualquier otro destino el contador de segundas citas vuelve a 0")
    void contadorVuelveACero() {
        Procedimiento pr03 = procedimiento(3L, "PR-03", false, EstadoProcedimiento.SEGUNDA_CITA, OPERACIONES, DESARROLLO, INFRA);
        pr03.setSegundasCitasConsecutivas(1);
        enJuego(pr03, 12, List.of(1L, 2L, 3L),
                new ConteoVotos(3L, 1L, ValorVoto.LIKE, 6), new ConteoVotos(3L, 1L, ValorVoto.NOPE, 1),
                new ConteoVotos(3L, 2L, ValorVoto.LIKE, 3), new ConteoVotos(3L, 2L, ValorVoto.NOPE, 1),
                new ConteoVotos(3L, 3L, ValorVoto.LIKE, 4), new ConteoVotos(3L, 3L, ValorVoto.NOPE, 1));

        var resumen = service.cerrar(4L, 13L);

        assertThat(pr03.getEstado()).isEqualTo(EstadoProcedimiento.PENDIENTE_CASAMIENTO);
        assertThat(pr03.getSegundasCitasConsecutivas()).isZero();
        assertThat(resultadoGuardado().isGrupal()).isTrue();
        assertThat(resumen.resultadosCalculados()).isEqualTo(3);
        verify(resultados, times(3)).save(any());
    }

    @Test
    @DisplayName("Votación rápida de CASAR_CON_CONDICIONES: con 70 % o más pasa a PENDIENTE_CASAMIENTO")
    void rapidaCasarConCondiciones() {
        Procedimiento f07 = procedimiento(7L, "F-07", false, EstadoProcedimiento.SEGUNDA_CITA, OPERACIONES);
        CicloVotacion rapida = CicloVotacion.rapida("Rápida F-07", f07, LocalDate.now(), LocalDate.now().plusDays(7));
        rapida.setId(9L);
        SegundaCita cita = new SegundaCita(f07, ciclo, LocalDate.now());
        cita.realizar("minuta", DecisionSegundaCita.CASAR_CON_CONDICIONES, "Una sola pregunta");
        when(ciclos.findById(9L)).thenReturn(Optional.of(rapida));
        when(citas.findFirstByCicloRapidoId(9L)).thenReturn(Optional.of(cita));
        when(votos.contarPorCiclo(9L)).thenReturn(List.of(
                new ConteoVotos(7L, 1L, ValorVoto.LIKE, 4), new ConteoVotos(7L, 1L, ValorVoto.NOPE, 1)));
        when(matriz.areasDelProcedimiento(7L)).thenReturn(List.of(1L));
        when(matriz.countByIdProcedimientoId(7L)).thenReturn(6L);

        service.cerrar(9L, 13L);

        assertThat(f07.getEstado()).isEqualTo(EstadoProcedimiento.PENDIENTE_CASAMIENTO);
        assertThat(resultadoGuardado().getDestino()).isEqualTo(Veredicto.CASAMIENTO);
        verify(procedimientos, never()).findByEstadoIn(any());
    }
}
