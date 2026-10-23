package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
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
    @Mock ParticipacionRepository participaciones;
    @Mock VotoRepository votos;
    @Mock ResultadoCicloRepository resultados;
    @InjectMocks TinderService tinder;

    private final UsuarioAutenticado facundo = new UsuarioAutenticado(6L, "Facundo", "facundo@demo.com", "x",
            Rol.EMPLEADO, DESARROLLO.getId(), DESARROLLO.getNombre());
    private CicloVotacion ciclo;
    private Procedimiento pr01;

    @BeforeEach
    void setUp() {
        ciclo = new CicloVotacion("2026-T4", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31));
        ciclo.setId(3L);
        pr01 = Fixtures.procedimiento(1L, "PR-01", false, EstadoProcedimiento.EN_TINDER, DESARROLLO);
        when(ciclos.findFirstByEstado(EstadoCiclo.ABIERTO)).thenReturn(Optional.of(ciclo));
        when(procedimientos.findById(1L)).thenReturn(Optional.of(pr01));
    }

    @Test
    @DisplayName("Primer voto: guarda participación y voto (sin usuario) y responde match en LIKE")
    void votoOk() {
        when(participaciones.existsById(any())).thenReturn(false);

        assertThat(tinder.votar(facundo, 1L, ValorVoto.LIKE).match()).isTrue();

        ArgumentCaptor<Participacion> part = ArgumentCaptor.forClass(Participacion.class);
        verify(participaciones).saveAndFlush(part.capture());
        assertThat(part.getValue().getId()).isEqualTo(new Participacion.Id(6L, 1L, 3L));

        ArgumentCaptor<Voto> voto = ArgumentCaptor.forClass(Voto.class);
        verify(votos).save(voto.capture());
        assertThat(voto.getValue().getValor()).isEqualTo(ValorVoto.LIKE);
        assertThat(voto.getValue().getAreaId()).isEqualTo(DESARROLLO.getId());
    }

    @Test
    @DisplayName("NOPE no dispara la animación de match")
    void nopeNoEsMatch() {
        when(participaciones.existsById(any())).thenReturn(false);
        assertThat(tinder.votar(facundo, 1L, ValorVoto.NOPE).match()).isFalse();
    }

    @Test
    @DisplayName("Doble voto en el mismo ciclo => 409 y no se guarda nada")
    void dobleVotoDevuelve409() {
        when(participaciones.existsById(new Participacion.Id(6L, 1L, 3L))).thenReturn(true);

        assertThatThrownBy(() -> tinder.votar(facundo, 1L, ValorVoto.NOPE))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(api.getCodigo()).isEqualTo("VOTO_DUPLICADO");
                });
        verify(participaciones, never()).saveAndFlush(any());
        verifyNoInteractions(votos);
    }

    @Test
    @DisplayName("Doble voto concurrente (choca la PK de participacion) => también 409")
    void dobleVotoConcurrenteDevuelve409() {
        when(participaciones.existsById(any())).thenReturn(false);
        when(participaciones.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("pk_participacion"));

        assertThatThrownBy(() -> tinder.votar(facundo, 1L, ValorVoto.LIKE))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verifyNoInteractions(votos);
    }
}
