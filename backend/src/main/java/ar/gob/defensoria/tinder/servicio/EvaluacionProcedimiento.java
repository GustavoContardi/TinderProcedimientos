package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.Veredicto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de evaluar un procedimiento al cerrar un ciclo.
 *
 * @param destino             CASAMIENTO, SEGUNDA_CITA, VELORIO, QUORUM_INSUFICIENTE o PARTICIPACION_INSUFICIENTE
 * @param pctGlobal           % de "me sirve" sumando todas las áreas (también las sin quórum)
 * @param votantes            votos emitidos
 * @param habilitados         usuarios en la matriz del procedimiento
 * @param grupal              casamiento en 2 o más áreas
 * @param relacionADistancia  un área con 70 % o más y otra con menos de 40 %
 * @param mensaje             explicación legible
 * @param areas               veredicto por área
 */
public record EvaluacionProcedimiento(
        Veredicto destino,
        BigDecimal pctGlobal,
        int votantes,
        int habilitados,
        boolean grupal,
        boolean relacionADistancia,
        String mensaje,
        List<Area> areas) {

    /** Votos de un área en el ciclo. */
    public record Conteo(Long areaId, String areaNombre, int likes, int nopes) {
        public int total() {
            return likes + nopes;
        }
    }

    public record Area(Conteo conteo, BigDecimal pct, Veredicto veredicto) {
    }

    public boolean cambiaEstado() {
        return destino != Veredicto.PARTICIPACION_INSUFICIENTE && destino != Veredicto.QUORUM_INSUFICIENTE;
    }
}
