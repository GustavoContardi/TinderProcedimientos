package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.dominio.Procedimiento;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static ar.gob.defensoria.tinder.dominio.EstadoProcedimiento.*;

/**
 * Única fuente de verdad del ciclo de vida de un procedimiento.
 * <pre>
 * BORRADOR ──publicar──▶ EN_TINDER / SEGUNDA_CITA ──casamiento──▶ PENDIENTE_CASAMIENTO ──▶ CASADO
 *                              │                                                 │ objeción fundada / aniversario
 *                              │                                                 ├──▶ SEGUNDA_CITA
 *                              │                                                 └──divorcio──▶ PENDIENTE_VELORIO
 *                              └──velorio──▶ PENDIENTE_VELORIO ──3 firmas──▶ VELADO (obsoleto)
 * </pre>
 * Un obligatorio por norma nunca va a PENDIENTE_VELORIO.
 */
@Component
public class MaquinaEstados {

    private static final Map<EstadoProcedimiento, Set<EstadoProcedimiento>> TRANSICIONES = new EnumMap<>(Map.of(
            BORRADOR, EnumSet.of(EN_TINDER),
            EN_TINDER, EnumSet.of(SEGUNDA_CITA, PENDIENTE_CASAMIENTO, PENDIENTE_VELORIO),
            SEGUNDA_CITA, EnumSet.of(EN_TINDER, PENDIENTE_CASAMIENTO, PENDIENTE_VELORIO),
            PENDIENTE_CASAMIENTO, EnumSet.of(CASADO),
            CASADO, EnumSet.of(PENDIENTE_VELORIO, SEGUNDA_CITA),
            PENDIENTE_VELORIO, EnumSet.of(VELADO),
            VELADO, EnumSet.noneOf(EstadoProcedimiento.class)));

    public boolean esValida(Procedimiento p, EstadoProcedimiento destino) {
        if (p.getEstado() == destino) {
            return true;
        }
        if (destino == PENDIENTE_VELORIO && p.isObligatorio()) {
            return false;
        }
        return TRANSICIONES.get(p.getEstado()).contains(destino);
    }

    /** Aplica la transición o lanza 422 con un mensaje claro. */
    public void transicionar(Procedimiento p, EstadoProcedimiento destino) {
        if (p.getEstado() == destino) {
            return;
        }
        if (destino == PENDIENTE_VELORIO && p.isObligatorio()) {
            throw ApiException.reglaNegocio("TRANSICION_INVALIDA",
                    p.getCodigo() + ": " + ClasificadorVeredicto.MENSAJE_OBLIGATORIO + ".");
        }
        if (!TRANSICIONES.get(p.getEstado()).contains(destino)) {
            throw ApiException.reglaNegocio("TRANSICION_INVALIDA",
                    "%s no puede pasar de %s a %s.%s".formatted(p.getCodigo(), legible(p.getEstado()), legible(destino),
                            p.getEstado() == VELADO ? " Ya descansa en paz." : ""));
        }
        p.setEstado(destino);
    }

    public static String legible(EstadoProcedimiento e) {
        return switch (e) {
            case BORRADOR -> "Borrador";
            case EN_TINDER -> "En Tinder";
            case SEGUNDA_CITA -> "Segunda cita";
            case PENDIENTE_CASAMIENTO -> "Pendiente de casamiento";
            case CASADO -> "Casado";
            case PENDIENTE_VELORIO -> "Pendiente de velorio";
            case VELADO -> "Velado";
        };
    }
}
