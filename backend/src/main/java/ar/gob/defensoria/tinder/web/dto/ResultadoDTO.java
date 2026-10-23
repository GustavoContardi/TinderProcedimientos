package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.Veredicto;

/**
 * Resultado agregado por procedimiento y área. Si no hubo quórum, los números van en null
 * (por anonimato no se muestran).
 */
public record ResultadoDTO(
        Long cicloId,
        String cicloNombre,
        Long procedimientoId,
        String codigo,
        String nombre,
        Long areaId,
        String areaNombre,
        Veredicto veredicto,
        Integer likes,
        Integer nopes,
        Integer total,
        Integer pctLike,
        Integer pctNope,
        String mensaje) {
}
