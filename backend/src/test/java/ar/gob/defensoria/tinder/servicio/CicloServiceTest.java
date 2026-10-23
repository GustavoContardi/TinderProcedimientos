package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    @Mock AlertaRepository alertas;

    private CicloService service;
    private CicloVotacion ciclo;

    @BeforeEach
    void setUp() {
        service = new CicloService(ciclos, procedimientos, votos, resultados, alertas,
                new ClasificadorVeredicto(props()), new MaquinaEstados());
        ciclo = new CicloVotacion("2026-T4", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31));
        ciclo.setId(3L);
        when(ciclos.findById(3L)).thenReturn(Optional.of(ciclo));
    }

    @Test
    @DisplayName("Al cerrar: un obligatorio rechazado en todas sus áreas va a SEGUNDA_CITA, nunca a PENDIENTE_VELORIO")
    void obligatorioRechazadoAlCerrar() {
        Procedimiento pr02 = procedimiento(2L, "PR-02", true, EstadoProcedimiento.EN_TINDER, DESARROLLO, INFRA);
        Procedimiento f08 = procedimiento(8L, "F-08", false, EstadoProcedimiento.EN_TINDER, OPERACIONES);
        when(procedimientos.findByEstadoIn(any())).thenReturn(List.of(pr02, f08));
        when(votos.contarPorCiclo(3L)).thenReturn(List.of(
                new ConteoVotos(2L, DESARROLLO.getId(), ValorVoto.NOPE, 4),
                new ConteoVotos(2L, INFRA.getId(), ValorVoto.NOPE, 3),
                new ConteoVotos(8L, OPERACIONES.getId(), ValorVoto.NOPE, 3)));

        var resumen = service.cerrar(3L);

        assertThat(pr02.getEstado()).isEqualTo(EstadoProcedimiento.SEGUNDA_CITA);
        assertThat(f08.getEstado()).isEqualTo(EstadoProcedimiento.PENDIENTE_VELORIO);
        assertThat(ciclo.getEstado()).isEqualTo(EstadoCiclo.CERRADO);
        assertThat(resumen.resultadosCalculados()).isEqualTo(3);
        verify(alertas).save(argThat(a -> a.getTipo() == TipoAlerta.OBLIGATORIO_NO_VELABLE));
    }

    @Test
    @DisplayName("Al cerrar: MATCH en un área y RECHAZO en otra => SEGUNDA_CITA y alerta de relación a distancia")
    void relacionADistanciaAlCerrar() {
        Procedimiento pr04 = procedimiento(4L, "PR-04", false, EstadoProcedimiento.EN_TINDER, DESARROLLO, INFRA);
        when(procedimientos.findByEstadoIn(any())).thenReturn(List.of(pr04));
        when(votos.contarPorCiclo(3L)).thenReturn(List.of(
                new ConteoVotos(4L, DESARROLLO.getId(), ValorVoto.NOPE, 3),
                new ConteoVotos(4L, INFRA.getId(), ValorVoto.LIKE, 3)));

        var resumen = service.cerrar(3L);

        assertThat(pr04.getEstado()).isEqualTo(EstadoProcedimiento.SEGUNDA_CITA);
        assertThat(resumen.transiciones()).singleElement()
                .satisfies(t -> assertThat(t.mensaje()).startsWith("Relación a distancia"));
        verify(alertas).save(argThat(a -> a.getTipo() == TipoAlerta.RELACION_A_DISTANCIA));
    }

    @Test
    @DisplayName("Al cerrar: MATCH en las 3 áreas => PENDIENTE_CASAMIENTO, candidato a casamiento grupal")
    void casamientoGrupalAlCerrar() {
        Procedimiento pr03 = procedimiento(3L, "PR-03", false, EstadoProcedimiento.EN_TINDER, OPERACIONES, DESARROLLO, INFRA);
        when(procedimientos.findByEstadoIn(any())).thenReturn(List.of(pr03));
        when(votos.contarPorCiclo(3L)).thenReturn(List.of(
                new ConteoVotos(3L, OPERACIONES.getId(), ValorVoto.LIKE, 3),
                new ConteoVotos(3L, DESARROLLO.getId(), ValorVoto.LIKE, 3),
                new ConteoVotos(3L, DESARROLLO.getId(), ValorVoto.NOPE, 1),
                new ConteoVotos(3L, INFRA.getId(), ValorVoto.LIKE, 3)));

        service.cerrar(3L);

        assertThat(pr03.getEstado()).isEqualTo(EstadoProcedimiento.PENDIENTE_CASAMIENTO);
        verify(resultados, times(3)).save(any());
        verify(alertas).save(argThat(a -> a.getTipo() == TipoAlerta.CANDIDATO_CASAMIENTO_GRUPAL));
    }
}
