package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.CategoriaObjecion;
import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;

import java.util.List;

/** Objeciones de un procedimiento agrupadas por categoría. */
public record GrupoObjecionesDTO(
        Long procedimientoId,
        String codigo,
        String nombre,
        EstadoProcedimiento estadoProcedimiento,
        int total,
        int pendientes,
        List<Categoria> categorias) {

    public record Categoria(CategoriaObjecion categoria, int cantidad, List<ObjecionDTO> objeciones) {
    }
}
