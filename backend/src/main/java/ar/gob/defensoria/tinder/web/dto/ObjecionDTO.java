package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.CategoriaObjecion;
import ar.gob.defensoria.tinder.dominio.EstadoObjecion;
import ar.gob.defensoria.tinder.dominio.OrigenObjecion;

import java.util.UUID;

/** Una objeción tal como la ve el juez de paz: sin autor (no existe) y sin área (para no delatar a nadie). */
public record ObjecionDTO(
        UUID id,
        Long procedimientoId,
        String cicloNombre,
        OrigenObjecion origen,
        CategoriaObjecion categoria,
        String texto,
        Boolean fundada,
        String respuesta,
        EstadoObjecion estado,
        Long mejoraId,
        String mejoraCodigo) {
}
