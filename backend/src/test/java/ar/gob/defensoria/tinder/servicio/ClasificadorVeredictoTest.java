package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.TinderProperties;
import ar.gob.defensoria.tinder.dominio.Veredicto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClasificadorVeredictoTest {

    private final ClasificadorVeredicto clasificador = new ClasificadorVeredicto(Fixtures.props());

    @ParameterizedTest(name = "{0} likes / {1} nopes => {2}")
    @CsvSource({
            "0, 0, QUORUM_INSUFICIENTE",
            "2, 0, QUORUM_INSUFICIENTE",
            "1, 1, QUORUM_INSUFICIENTE",
            "3, 0, MATCH",
            "3, 2, MATCH",          // 60% exacto => MATCH
            "0, 3, RECHAZO",
            "2, 3, RECHAZO",        // 60% nope exacto => RECHAZO
            "1, 1, QUORUM_INSUFICIENTE",
            "2, 2, SEGUNDA_CITA",   // 50/50
            "5, 4, SEGUNDA_CITA",   // 55%
            "4, 5, SEGUNDA_CITA"
    })
    @DisplayName("Clasifica por quórum y umbrales")
    void clasificaPorUmbralesYQuorum(int likes, int nopes, Veredicto esperado) {
        assertThat(clasificador.clasificar(likes, nopes)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("El quórum y los umbrales son configurables")
    void parametrosConfigurables() {
        var exigente = new ClasificadorVeredicto(new TinderProperties(5, 0.80, 0.70, 15, 6, List.of()));
        assertThat(exigente.clasificar(4, 0)).isEqualTo(Veredicto.QUORUM_INSUFICIENTE);
        assertThat(exigente.clasificar(4, 1)).isEqualTo(Veredicto.MATCH);
        assertThat(exigente.clasificar(3, 2)).isEqualTo(Veredicto.SEGUNDA_CITA);
        assertThat(exigente.clasificar(1, 4)).isEqualTo(Veredicto.RECHAZO);
    }

    @Test
    void calculaPorcentajeDeLikes() {
        assertThat(ClasificadorVeredicto.pctLike(1, 3)).isEqualByComparingTo(new BigDecimal("0.25"));
        assertThat(ClasificadorVeredicto.pctLike(0, 0)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
