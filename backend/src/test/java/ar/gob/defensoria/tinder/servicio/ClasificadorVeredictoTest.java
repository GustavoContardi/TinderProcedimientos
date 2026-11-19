package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.TinderProperties;
import ar.gob.defensoria.tinder.dominio.Veredicto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static ar.gob.defensoria.tinder.dominio.Veredicto.*;
import static ar.gob.defensoria.tinder.servicio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class ClasificadorVeredictoTest {

    private final ClasificadorVeredicto clasificador = new ClasificadorVeredicto(props());

    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource({
            "0.70,  CASAMIENTO",
            "0.699, SEGUNDA_CITA",
            "0.40,  SEGUNDA_CITA",
            "0.399, VELORIO",
            "1.00,  CASAMIENTO",
            "0.00,  VELORIO"
    })
    @DisplayName("Umbrales 70 / 40: 70 % casa, 69,9 % segunda cita, 40 % segunda cita, 39,9 % velorio")
    void umbrales(double pct, Veredicto esperado) {
        assertThat(clasificador.clasificarPct(pct)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "{0} likes / {1} nopes => {2}")
    @CsvSource({
            "0, 0, QUORUM_INSUFICIENTE",
            "2, 0, QUORUM_INSUFICIENTE",
            "7, 3, CASAMIENTO",     // 70 % exacto
            "4, 6, SEGUNDA_CITA",   // 40 % exacto
            "3, 5, VELORIO",        // 37,5 %
            "0, 3, VELORIO"
    })
    @DisplayName("Veredicto por área: quórum de 3 votos y umbrales sin sorpresas de punto flotante")
    void veredictoPorArea(int likes, int nopes, Veredicto esperado) {
        assertThat(clasificador.clasificar(likes, nopes)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Todas las áreas con 70 % o más => CASAMIENTO, grupal si son 2 o más")
    void casamientoGrupal() {
        var ev = clasificador.evaluar(false, null, 14, List.of(
                conteo(OPERACIONES, 6, 1), conteo(DESARROLLO, 3, 1), conteo(INFRA, 4, 1)));
        assertThat(ev.destino()).isEqualTo(CASAMIENTO);
        assertThat(ev.grupal()).isTrue();
        assertThat(ev.relacionADistancia()).isFalse();
    }

    @Test
    @DisplayName("Todas las áreas con menos de 40 % => VELORIO")
    void velorio() {
        var ev = clasificador.evaluar(false, null, 10, List.of(conteo(OPERACIONES, 1, 4), conteo(DESARROLLO, 0, 3)));
        assertThat(ev.destino()).isEqualTo(VELORIO);
    }

    @Test
    @DisplayName("Un obligatorio con 10 % => SEGUNDA_CITA (se simplifica, no se vela)")
    void obligatorioCon10PorCiento() {
        var ev = clasificador.evaluar(true, "ISO 9001 7.1.3", 10, List.of(conteo(DESARROLLO, 1, 9)));
        assertThat(ev.pctGlobal()).isEqualByComparingTo("0.10");
        assertThat(ev.destino()).isEqualTo(SEGUNDA_CITA);
        assertThat(ev.mensaje()).startsWith("Obligatorio por norma: se simplifica, no se vela");
    }

    @Test
    @DisplayName("Áreas con 85 % y 20 % => SEGUNDA_CITA con relación a distancia")
    void relacionADistancia() {
        var ev = clasificador.evaluar(false, null, 30, List.of(conteo(INFRA, 17, 3), conteo(DESARROLLO, 2, 8)));
        assertThat(ev.areas()).extracting(EvaluacionProcedimiento.Area::veredicto).containsExactly(CASAMIENTO, VELORIO);
        assertThat(ev.destino()).isEqualTo(SEGUNDA_CITA);
        assertThat(ev.relacionADistancia()).isTrue();
        assertThat(ev.mensaje()).contains("Relación a distancia").contains("Infraestructura").contains("Desarrollo");
    }

    @Test
    @DisplayName("Votó el 40 % de la matriz => PARTICIPACION_INSUFICIENTE")
    void participacionInsuficiente() {
        var ev = clasificador.evaluar(false, null, 10, List.of(conteo(OPERACIONES, 4, 0)));
        assertThat(ev.destino()).isEqualTo(PARTICIPACION_INSUFICIENTE);
        assertThat(ev.cambiaEstado()).isFalse();
    }

    @Test
    @DisplayName("Con exactamente el 50 % de participación se evalúa")
    void participacionJusta() {
        var ev = clasificador.evaluar(false, null, 8, List.of(conteo(OPERACIONES, 4, 0)));
        assertThat(ev.destino()).isEqualTo(CASAMIENTO);
    }

    @Test
    @DisplayName("Áreas sin quórum no deciden pero sus votos suman al global")
    void areaSinQuorumSumaAlGlobal() {
        var ev = clasificador.evaluar(false, null, 6, List.of(conteo(OPERACIONES, 3, 1), conteo(INFRA, 2, 0)));
        assertThat(ev.areas()).extracting(EvaluacionProcedimiento.Area::veredicto)
                .containsExactly(CASAMIENTO, QUORUM_INSUFICIENTE);
        assertThat(ev.destino()).isEqualTo(CASAMIENTO);
        assertThat(ev.grupal()).isFalse();
        assertThat(ev.votantes()).isEqualTo(6);
        assertThat(ev.pctGlobal()).isEqualByComparingTo("0.8333");
    }

    @Test
    @DisplayName("Ningún área con quórum: decide el global si hay 3 votos o más; si no, QUORUM_INSUFICIENTE")
    void sinQuorumEnNingunArea() {
        var global = clasificador.evaluar(false, null, 4, List.of(conteo(OPERACIONES, 1, 1), conteo(INFRA, 0, 2)));
        assertThat(global.destino()).isEqualTo(VELORIO);

        var pocos = clasificador.evaluar(false, null, 4, List.of(conteo(OPERACIONES, 1, 1)));
        assertThat(pocos.destino()).isEqualTo(QUORUM_INSUFICIENTE);
    }

    @Test
    @DisplayName("Los umbrales, el quórum y la participación son configurables")
    void parametrosConfigurables() {
        var exigente = new ClasificadorVeredicto(new TinderProperties(0.80, 0.30, 5, 0.75, 15, 6, 30, 7, 7, List.of()));
        assertThat(exigente.clasificar(4, 0)).isEqualTo(QUORUM_INSUFICIENTE);
        assertThat(exigente.clasificar(4, 1)).isEqualTo(CASAMIENTO);
        assertThat(exigente.clasificar(3, 2)).isEqualTo(SEGUNDA_CITA);
        assertThat(exigente.clasificar(1, 4)).isEqualTo(VELORIO);
        assertThat(exigente.hayParticipacion(7, 10)).isFalse();
    }

    @Test
    void calculaPorcentaje() {
        assertThat(ClasificadorVeredicto.pct(1, 3)).isEqualByComparingTo(new BigDecimal("0.25"));
        assertThat(ClasificadorVeredicto.pct(0, 0)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
