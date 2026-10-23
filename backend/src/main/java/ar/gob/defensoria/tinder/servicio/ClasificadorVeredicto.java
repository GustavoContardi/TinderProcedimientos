package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.TinderProperties;
import ar.gob.defensoria.tinder.dominio.Veredicto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Aplica quórum y umbrales (configurables) para decidir el veredicto de un procedimiento en un área. */
@Component
public class ClasificadorVeredicto {

    private final TinderProperties props;

    public ClasificadorVeredicto(TinderProperties props) {
        this.props = props;
    }

    public Veredicto clasificar(int likes, int nopes) {
        int total = likes + nopes;
        if (total < props.quorumMinimo()) {
            return Veredicto.QUORUM_INSUFICIENTE;
        }
        double pctLike = (double) likes / total;
        double pctNope = (double) nopes / total;
        // Pequeña tolerancia para que 3/5 = 0.6 cuente como >= 0.60 sin sorpresas de punto flotante.
        if (pctLike + 1e-9 >= props.umbralMatch()) {
            return Veredicto.MATCH;
        }
        if (pctNope + 1e-9 >= props.umbralRechazo()) {
            return Veredicto.RECHAZO;
        }
        return Veredicto.SEGUNDA_CITA;
    }

    public static BigDecimal pctLike(int likes, int nopes) {
        int total = likes + nopes;
        if (total == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(likes).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }
}
