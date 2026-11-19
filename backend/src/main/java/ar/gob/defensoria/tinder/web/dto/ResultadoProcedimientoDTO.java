package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.TipoCiclo;
import ar.gob.defensoria.tinder.dominio.Veredicto;

import java.util.List;

/**
 * Resultado de un procedimiento en un ciclo, con sus áreas. destino es null en ciclos anteriores a la v2
 * (no se recalculan). pctGlobal es null si no hubo quórum (por anonimato).
 */
public record ResultadoProcedimientoDTO(
        Long cicloId,
        String cicloNombre,
        TipoCiclo cicloTipo,
        Long procedimientoId,
        String codigo,
        String nombre,
        Veredicto destino,
        Integer pctGlobal,
        Integer votantes,
        Integer habilitados,
        Integer participacionPct,
        boolean grupal,
        boolean relacionADistancia,
        String mensaje,
        List<ResultadoDTO> areas) {
}
