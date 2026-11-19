package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.ObjecionRequest;
import ar.gob.defensoria.tinder.web.dto.VotoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static ar.gob.defensoria.tinder.servicio.Fixtures.DESARROLLO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TinderServiceTest {

    @Mock ProcedimientoRepository procedimientos;
    @Mock CicloVotacionRepository ciclos;
    @Mock VotoEmitidoRepository votosEmitidos;
    @Mock VotoRepository votos;
    @Mock ObjecionRepository objeciones;
    @Mock MatrizParticipacionRepository matriz;
    @Mock ResultadoCicloRepository resultados;
    @InjectMocks TinderService tinder;

    private final UsuarioAutenticado facundo = new UsuarioAutenticado(6L, "Facundo", "facundo@demo.com", "x",
            Rol.EMPLEADO, DESARROLLO.getId(), DESARROLLO.getNombre());
    private CicloVotacion ciclo;
    private Procedimiento pr01;

    @BeforeEach
    void setUp() {
        ciclo = new CicloVotacion("2026-T4", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31));
        ciclo.setId(4L);
        pr01 = Fixtures.procedimiento(1L, "PR-01", false, EstadoProcedimiento.EN_TINDER, DESARROLLO);
        lenient().when(ciclos.findById(4L)).thenReturn(Optional.of(ciclo));
        lenient().when(procedimientos.findById(1L)).thenReturn(Optional.of(pr01));
        lenient().when(matriz.existsById(new MatrizParticipacion.Id(6L, 1L))).thenReturn(true);
    }

    private static VotoRequest voto(ValorVoto valor, ObjecionRequest objecion) {
        return new VotoRequest(1L, 4L, valor, objecion);
    }

    @Test
    @DisplayName("NOPE con objeción: guarda voto_emitido, voto y objeción en la misma transacción, ninguno con usuario")
    void votoConObjecion() {
        assertThat(tinder.votar(facundo, voto(ValorVoto.NOPE,
                new ObjecionRequest(CategoriaObjecion.DEMASIADOS_PASOS, "  Son muchos pasos "))).match()).isFalse();

        verify(votosEmitidos).saveAndFlush(argThat(e -> e.getId().equals(new VotoEmitido.Id(6L, 1L, 4L))));
        ArgumentCaptor<Voto> v = ArgumentCaptor.forClass(Voto.class);
        verify(votos).save(v.capture());
        assertThat(v.getValue().getValor()).isEqualTo(ValorVoto.NOPE);
        assertThat(v.getValue().getAreaId()).isEqualTo(DESARROLLO.getId());
        ArgumentCaptor<Objecion> o = ArgumentCaptor.forClass(Objecion.class);
        verify(objeciones).save(o.capture());
        assertThat(o.getValue().getOrigen()).isEqualTo(OrigenObjecion.VOTO);
        assertThat(o.getValue().getCategoria()).isEqualTo(CategoriaObjecion.DEMASIADOS_PASOS);
        assertThat(o.getValue().getTexto()).isEqualTo("Son muchos pasos");
        assertThat(o.getValue().getCicloId()).isEqualTo(4L);
        assertThat(o.getValue().getEstado()).isEqualTo(EstadoObjecion.PENDIENTE);
    }

    @Test
    @DisplayName("LIKE sin objeción responde match y no guarda objeción")
    void likeEsMatch() {
        assertThat(tinder.votar(facundo, voto(ValorVoto.LIKE, null)).match()).isTrue();
        verifyNoInteractions(objeciones);
    }

    @Test
    @DisplayName("Votar un procedimiento fuera de la matriz => 403 y no se guarda nada")
    void fueraDeMatriz() {
        when(matriz.existsById(new MatrizParticipacion.Id(6L, 1L))).thenReturn(false);

        assertThatThrownBy(() -> tinder.votar(facundo, voto(ValorVoto.LIKE, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Solo podés votar los procedimientos en los que participás")
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(votosEmitidos, votos, objeciones);
    }

    @Test
    @DisplayName("Objeción enviada con LIKE => 400")
    void objecionConLike() {
        assertThatThrownBy(() -> tinder.votar(facundo, voto(ValorVoto.LIKE, new ObjecionRequest(CategoriaObjecion.OTRO, null))))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(votosEmitidos, votos, objeciones);
    }

    @Test
    @DisplayName("Doble voto en el mismo ciclo => 409 y no se guarda nada")
    void dobleVotoDevuelve409() {
        when(votosEmitidos.existsById(new VotoEmitido.Id(6L, 1L, 4L))).thenReturn(true);

        assertThatThrownBy(() -> tinder.votar(facundo, voto(ValorVoto.NOPE, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(api.getCodigo()).isEqualTo("VOTO_DUPLICADO");
                });
        verify(votosEmitidos, never()).saveAndFlush(any());
        verifyNoInteractions(votos, objeciones);
    }

    @Test
    @DisplayName("Doble voto concurrente (choca la PK de voto_emitido) => también 409")
    void dobleVotoConcurrenteDevuelve409() {
        when(votosEmitidos.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("pk_voto_emitido"));

        assertThatThrownBy(() -> tinder.votar(facundo, voto(ValorVoto.LIKE, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verifyNoInteractions(votos, objeciones);
    }

    @Test
    @DisplayName("Con una votación rápida abierta, el procedimiento no se vota en el trimestral")
    void conRapidaAbiertaNoSeVotaEnTrimestral() {
        pr01.setEstado(EstadoProcedimiento.SEGUNDA_CITA);
        when(ciclos.existsByProcedimientoIdAndEstadoAndTipo(1L, EstadoCiclo.ABIERTO, TipoCiclo.RAPIDA)).thenReturn(true);

        assertThatThrownBy(() -> tinder.votar(facundo, voto(ValorVoto.LIKE, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }
}
