package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.dominio.Procedimiento;
import ar.gob.defensoria.tinder.dominio.TipoAlerta;
import ar.gob.defensoria.tinder.dominio.Veredicto;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static ar.gob.defensoria.tinder.dominio.EstadoProcedimiento.*;

/**
 * Única fuente de verdad del ciclo de vida de un procedimiento.
 * <pre>
 * EN_TINDER / SEGUNDA_CITA ──match──▶ PENDIENTE_CASAMIENTO ──▶ CASADO ──divorcio──▶ PENDIENTE_VELORIO ──firma──▶ VELADO
 *            │                                                    └──divorcio (obligatorio)──▶ SEGUNDA_CITA
 *            └──rechazo──▶ PENDIENTE_VELORIO (si es obligatorio: SEGUNDA_CITA)
 * </pre>
 */
@Component
public class MaquinaEstados {

    public static final String MENSAJE_OBLIGATORIO = "Es obligatorio por norma: no se puede velar, se simplifica";

    private static final Map<EstadoProcedimiento, Set<EstadoProcedimiento>> TRANSICIONES = new EnumMap<>(Map.of(
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
                    p.getCodigo() + ": " + MENSAJE_OBLIGATORIO + ".");
        }
        if (!TRANSICIONES.get(p.getEstado()).contains(destino)) {
            throw ApiException.reglaNegocio("TRANSICION_INVALIDA",
                    "%s no puede pasar de %s a %s.%s".formatted(p.getCodigo(), legible(p.getEstado()), legible(destino),
                            p.getEstado() == VELADO ? " Ya descansa en paz." : ""));
        }
        p.setEstado(destino);
    }

    /**
     * Consolida los veredictos por área (solo cuentan las áreas con quórum) y decide el próximo estado.
     *
     * @param veredictosPorArea nombre del área → veredicto de ese área en el ciclo
     */
    public DecisionCierre resolverCierre(Procedimiento p, Map<String, Veredicto> veredictosPorArea) {
        Map<String, Veredicto> conQuorum = veredictosPorArea.entrySet().stream()
                .filter(e -> e.getValue() != Veredicto.QUORUM_INSUFICIENTE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        if (conQuorum.isEmpty()) {
            return new DecisionCierre(p.getEstado(), null,
                    "Sin quórum en ninguna área: sigue en el Tinder esperando más votos.");
        }

        Collection<Veredicto> v = conQuorum.values();
        if (v.stream().allMatch(x -> x == Veredicto.MATCH)) {
            if (conQuorum.size() >= 2) {
                return new DecisionCierre(PENDIENTE_CASAMIENTO, TipoAlerta.CANDIDATO_CASAMIENTO_GRUPAL,
                        "Match en " + conQuorum.size() + " áreas (" + String.join(", ", conQuorum.keySet())
                                + "): candidato a casamiento grupal.");
            }
            return new DecisionCierre(PENDIENTE_CASAMIENTO, null, "¡Match! Preparen la libreta.");
        }

        if (v.stream().allMatch(x -> x == Veredicto.RECHAZO)) {
            if (p.isObligatorio()) {
                return new DecisionCierre(SEGUNDA_CITA, TipoAlerta.OBLIGATORIO_NO_VELABLE, MENSAJE_OBLIGATORIO
                        + (p.getBaseObligatoriedad() != null ? " (" + p.getBaseObligatoriedad() + ")." : "."));
            }
            return new DecisionCierre(PENDIENTE_VELORIO, null, "Rechazo unánime: vayan comprando las flores.");
        }

        if (v.contains(Veredicto.MATCH) && v.contains(Veredicto.RECHAZO)) {
            String matchEn = areasCon(conQuorum, Veredicto.MATCH);
            String rechazoEn = areasCon(conQuorum, Veredicto.RECHAZO);
            return new DecisionCierre(SEGUNDA_CITA, TipoAlerta.RELACION_A_DISTANCIA,
                    "Relación a distancia: match en " + matchEn + " y rechazo en " + rechazoEn
                            + ". Calidad tiene que revisarlo.");
        }

        return new DecisionCierre(SEGUNDA_CITA, null,
                "Votos divididos: se simplifica y vuelve a votarse el trimestre siguiente.");
    }

    private static String areasCon(Map<String, Veredicto> m, Veredicto v) {
        return m.entrySet().stream().filter(e -> e.getValue() == v).map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));
    }

    public static String legible(EstadoProcedimiento e) {
        return switch (e) {
            case EN_TINDER -> "En Tinder";
            case SEGUNDA_CITA -> "Segunda cita";
            case PENDIENTE_CASAMIENTO -> "Pendiente de casamiento";
            case CASADO -> "Casado";
            case PENDIENTE_VELORIO -> "Pendiente de velorio";
            case VELADO -> "Velado";
        };
    }
}
