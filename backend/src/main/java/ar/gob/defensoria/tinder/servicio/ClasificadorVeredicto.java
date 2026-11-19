package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.TinderProperties;
import ar.gob.defensoria.tinder.dominio.Veredicto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import static ar.gob.defensoria.tinder.dominio.Veredicto.*;

/**
 * Aplica participación, quórum y umbrales (configurables, 70 / 40 por defecto) para decidir
 * el veredicto de cada área y el destino del procedimiento.
 */
@Component
public class ClasificadorVeredicto {

    public static final String MENSAJE_OBLIGATORIO = "Obligatorio por norma: se simplifica, no se vela";

    /** Tolerancia para que 7/10 cuente como 70 % sin sorpresas de punto flotante. */
    private static final double EPS = 1e-9;

    private final TinderProperties props;

    public ClasificadorVeredicto(TinderProperties props) {
        this.props = props;
    }

    /** Veredicto por porcentaje de "me sirve" (0 a 1), sin mirar quórum. */
    public Veredicto clasificarPct(double pct) {
        if (pct + EPS >= props.umbralCasamiento()) {
            return CASAMIENTO;
        }
        if (pct + EPS < props.umbralVelorio()) {
            return VELORIO;
        }
        return SEGUNDA_CITA;
    }

    /** Veredicto de un área: con menos de N votos es QUORUM_INSUFICIENTE. */
    public Veredicto clasificar(int likes, int nopes) {
        int total = likes + nopes;
        if (total < props.quorumArea()) {
            return QUORUM_INSUFICIENTE;
        }
        return clasificarPct((double) likes / total);
    }

    public boolean alcanzaCasamiento(BigDecimal pct) {
        return pct.doubleValue() + EPS >= props.umbralCasamiento();
    }

    public boolean caeEnVelorio(BigDecimal pct) {
        return pct.doubleValue() + EPS < props.umbralVelorio();
    }

    public boolean hayQuorum(int votos) {
        return votos >= props.quorumArea();
    }

    public boolean hayParticipacion(int votantes, int habilitados) {
        return habilitados > 0 && votantes + EPS >= props.participacionMinima() * habilitados;
    }

    /**
     * Algoritmo de cierre por procedimiento.
     * <ol>
     *   <li>votantes / habilitados &lt; participación mínima → PARTICIPACION_INSUFICIENTE.</li>
     *   <li>Áreas con menos de N votos → QUORUM_INSUFICIENTE (sus votos igual suman al global).</li>
     *   <li>Se decide con las áreas con quórum; si no hay ninguna, con el global (si hay N votos o más).</li>
     *   <li>Todas ≥ 70 % → CASAMIENTO (grupal si son 2+); todas &lt; 40 % → VELORIO; si no, SEGUNDA_CITA
     *       (relación a distancia si una tiene ≥ 70 % y otra &lt; 40 %).</li>
     *   <li>Obligatorio: el VELORIO se convierte en SEGUNDA_CITA.</li>
     * </ol>
     */
    public EvaluacionProcedimiento evaluar(boolean obligatorio, String baseObligatoriedad, int habilitados,
                                           List<EvaluacionProcedimiento.Conteo> conteos) {
        int likes = conteos.stream().mapToInt(EvaluacionProcedimiento.Conteo::likes).sum();
        int votantes = conteos.stream().mapToInt(EvaluacionProcedimiento.Conteo::total).sum();
        BigDecimal pctGlobal = pct(likes, votantes - likes);

        List<EvaluacionProcedimiento.Area> areas = conteos.stream()
                .map(c -> new EvaluacionProcedimiento.Area(c, pct(c.likes(), c.nopes()), clasificar(c.likes(), c.nopes())))
                .toList();

        if (!hayParticipacion(votantes, habilitados)) {
            int pctPart = habilitados == 0 ? 0 : (int) Math.round(100.0 * votantes / habilitados);
            return new EvaluacionProcedimiento(PARTICIPACION_INSUFICIENTE, pctGlobal, votantes, habilitados, false, false,
                    "Votó el " + pctPart + " % de la matriz (mínimo " + Math.round(props.participacionMinima() * 100)
                            + " %): queda como estaba.", areas);
        }

        List<EvaluacionProcedimiento.Area> conQuorum = areas.stream()
                .filter(a -> a.veredicto() != QUORUM_INSUFICIENTE).toList();
        List<Veredicto> veredictos;
        if (!conQuorum.isEmpty()) {
            veredictos = conQuorum.stream().map(EvaluacionProcedimiento.Area::veredicto).toList();
        } else if (hayQuorum(votantes)) {
            veredictos = List.of(clasificarPct(pctGlobal.doubleValue()));
        } else {
            return new EvaluacionProcedimiento(QUORUM_INSUFICIENTE, pctGlobal, votantes, habilitados, false, false,
                    "Menos de " + props.quorumArea() + " votos: por anonimato no mostramos números y queda como estaba.",
                    areas);
        }

        if (veredictos.stream().allMatch(v -> v == CASAMIENTO)) {
            boolean grupal = conQuorum.size() >= 2;
            String mensaje = grupal
                    ? "Casamiento grupal: 70 % o más en " + nombres(conQuorum, CASAMIENTO) + "."
                    : "¡Es un match! Preparen la libreta.";
            return new EvaluacionProcedimiento(CASAMIENTO, pctGlobal, votantes, habilitados, grupal, false, mensaje, areas);
        }
        if (veredictos.stream().allMatch(v -> v == VELORIO)) {
            if (obligatorio) {
                return new EvaluacionProcedimiento(SEGUNDA_CITA, pctGlobal, votantes, habilitados, false, false,
                        MENSAJE_OBLIGATORIO + (baseObligatoriedad != null ? " (" + baseObligatoriedad + ")." : "."), areas);
            }
            return new EvaluacionProcedimiento(VELORIO, pctGlobal, votantes, habilitados, false, false,
                    "Menos del 40 % en todas las áreas: vayan comprando las flores.", areas);
        }
        boolean distancia = veredictos.contains(CASAMIENTO) && veredictos.contains(VELORIO);
        String mensaje = distancia
                ? "Relación a distancia: le sirve a " + nombres(conQuorum, CASAMIENTO) + " y le sobra a "
                + nombres(conQuorum, VELORIO) + ". Segunda cita."
                : "Votos divididos: segunda cita para escuchar a la minoría.";
        return new EvaluacionProcedimiento(SEGUNDA_CITA, pctGlobal, votantes, habilitados, false, distancia, mensaje, areas);
    }

    private static String nombres(List<EvaluacionProcedimiento.Area> areas, Veredicto v) {
        return areas.stream().filter(a -> a.veredicto() == v).map(a -> a.conteo().areaNombre())
                .collect(Collectors.joining(", "));
    }

    public static BigDecimal pct(int likes, int nopes) {
        int total = likes + nopes;
        if (total == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(likes).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }
}
