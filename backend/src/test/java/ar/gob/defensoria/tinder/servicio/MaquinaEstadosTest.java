package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.dominio.Procedimiento;
import ar.gob.defensoria.tinder.dominio.TipoAlerta;
import ar.gob.defensoria.tinder.dominio.Veredicto;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Map;

import static ar.gob.defensoria.tinder.dominio.EstadoProcedimiento.*;
import static ar.gob.defensoria.tinder.servicio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaquinaEstadosTest {

    private final MaquinaEstados maquina = new MaquinaEstados();

    private static Map<String, Veredicto> veredictos(Object... pares) {
        Map<String, Veredicto> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            m.put((String) pares[i], (Veredicto) pares[i + 1]);
        }
        return m;
    }

    @Test
    @DisplayName("MATCH en todas las áreas con quórum => PENDIENTE_CASAMIENTO, grupal si son 2+")
    void matchEnTodasLasAreas() {
        Procedimiento p = procedimiento(1L, "PR-03", false, EN_TINDER, OPERACIONES, DESARROLLO, INFRA);
        DecisionCierre d = maquina.resolverCierre(p, veredictos(
                "Operaciones", Veredicto.MATCH, "Desarrollo", Veredicto.MATCH, "Infra", Veredicto.QUORUM_INSUFICIENTE));
        assertThat(d.destino()).isEqualTo(PENDIENTE_CASAMIENTO);
        assertThat(d.alerta()).isEqualTo(TipoAlerta.CANDIDATO_CASAMIENTO_GRUPAL);
    }

    @Test
    @DisplayName("RECHAZO en todas sus áreas => PENDIENTE_VELORIO")
    void rechazoUnanime() {
        Procedimiento p = procedimiento(1L, "F-08", false, EN_TINDER, OPERACIONES);
        DecisionCierre d = maquina.resolverCierre(p, veredictos("Operaciones", Veredicto.RECHAZO));
        assertThat(d.destino()).isEqualTo(PENDIENTE_VELORIO);
    }

    @Test
    @DisplayName("Obligatorio rechazado => SEGUNDA_CITA y nunca PENDIENTE_VELORIO")
    void obligatorioRechazadoVaASegundaCita() {
        Procedimiento p = procedimiento(2L, "PR-02", true, EN_TINDER, DESARROLLO, INFRA);
        DecisionCierre d = maquina.resolverCierre(p, veredictos(
                "Desarrollo", Veredicto.RECHAZO, "Infraestructura", Veredicto.RECHAZO));
        assertThat(d.destino()).isEqualTo(SEGUNDA_CITA);
        assertThat(d.alerta()).isEqualTo(TipoAlerta.OBLIGATORIO_NO_VELABLE);
        assertThat(d.mensaje()).contains("Es obligatorio por norma: no se puede velar, se simplifica");
    }

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
    @DisplayName("MATCH en un área y RECHAZO en otra => SEGUNDA_CITA (relación a distancia) con alerta")
    void relacionADistancia() {
        Procedimiento p = procedimiento(4L, "PR-04", false, EN_TINDER, DESARROLLO, INFRA);
        DecisionCierre d = maquina.resolverCierre(p, veredictos(
                "Desarrollo", Veredicto.RECHAZO, "Infraestructura", Veredicto.MATCH));
        assertThat(d.destino()).isEqualTo(SEGUNDA_CITA);
        assertThat(d.alerta()).isEqualTo(TipoAlerta.RELACION_A_DISTANCIA);
        assertThat(d.mensaje()).contains("Relación a distancia").contains("Infraestructura").contains("Desarrollo");
    }

    @Test
    @DisplayName("Sin quórum en ninguna área => queda donde estaba (en el Tinder)")
    void sinQuorum() {
        Procedimiento p = procedimiento(1L, "PR-01", false, EN_TINDER, DESARROLLO);
        DecisionCierre d = maquina.resolverCierre(p, veredictos("Desarrollo", Veredicto.QUORUM_INSUFICIENTE));
        assertThat(d.destino()).isEqualTo(EN_TINDER);
        assertThat(d.alerta()).isNull();
    }

    @Test
    @DisplayName("Votos divididos => SEGUNDA_CITA")
    void votosDivididos() {
        Procedimiento p = procedimiento(9L, "F-09", false, EN_TINDER, DESARROLLO);
        assertThat(maquina.resolverCierre(p, veredictos("Desarrollo", Veredicto.SEGUNDA_CITA)).destino())
                .isEqualTo(SEGUNDA_CITA);
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
