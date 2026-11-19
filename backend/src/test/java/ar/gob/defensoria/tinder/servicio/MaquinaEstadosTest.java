package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.dominio.Procedimiento;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static ar.gob.defensoria.tinder.dominio.EstadoProcedimiento.*;
import static ar.gob.defensoria.tinder.servicio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaquinaEstadosTest {

    private final MaquinaEstados maquina = new MaquinaEstados();

    @Test
    @DisplayName("La máquina de estados prohíbe mandar un obligatorio a PENDIENTE_VELORIO (422)")
    void obligatorioNuncaPendienteDeVelorio() {
        for (EstadoProcedimiento origen : new EstadoProcedimiento[]{EN_TINDER, SEGUNDA_CITA, CASADO}) {
            Procedimiento p = procedimiento(2L, "PR-02", true, origen, DESARROLLO);
            assertThatThrownBy(() -> maquina.transicionar(p, PENDIENTE_VELORIO))
                    .isInstanceOf(ApiException.class)
                    .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
            assertThat(p.getEstado()).isEqualTo(origen);
        }
    }

    @Test
    @DisplayName("Transiciones inválidas devuelven 422")
    void transicionesInvalidas() {
        Procedimiento velado = procedimiento(10L, "F-23", false, VELADO, OPERACIONES);
        assertThatThrownBy(() -> maquina.transicionar(velado, EN_TINDER))
                .isInstanceOf(ApiException.class).hasMessageContaining("descansa en paz");

        Procedimiento enTinder = procedimiento(1L, "PR-01", false, EN_TINDER, DESARROLLO);
        assertThatThrownBy(() -> maquina.transicionar(enTinder, CASADO)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> maquina.transicionar(enTinder, VELADO)).isInstanceOf(ApiException.class);

        Procedimiento borrador = procedimiento(11L, "PR-11", false, BORRADOR, OPERACIONES);
        assertThatThrownBy(() -> maquina.transicionar(borrador, PENDIENTE_CASAMIENTO)).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("Un BORRADOR solo se publica (pasa a EN_TINDER)")
    void borradorSePublica() {
        Procedimiento p = procedimiento(11L, "PR-11", false, BORRADOR, OPERACIONES);
        assertThat(maquina.esValida(p, EN_TINDER)).isTrue();
        maquina.transicionar(p, EN_TINDER);
        assertThat(p.getEstado()).isEqualTo(EN_TINDER);
    }

    @Test
    @DisplayName("Una objeción fundada en la boda manda al casado de vuelta a segunda cita")
    void casadoVuelveASegundaCita() {
        Procedimiento p = procedimiento(3L, "PR-03", false, CASADO, OPERACIONES);
        maquina.transicionar(p, SEGUNDA_CITA);
        assertThat(p.getEstado()).isEqualTo(SEGUNDA_CITA);
    }

    @Test
    @DisplayName("Camino feliz completo: Tinder → casamiento → divorcio → velorio")
    void caminoCompleto() {
        Procedimiento p = procedimiento(1L, "PR-01", false, EN_TINDER, DESARROLLO);
        maquina.transicionar(p, PENDIENTE_CASAMIENTO);
        maquina.transicionar(p, CASADO);
        maquina.transicionar(p, PENDIENTE_VELORIO);
        maquina.transicionar(p, VELADO);
        assertThat(p.getEstado()).isEqualTo(VELADO);
    }
}
