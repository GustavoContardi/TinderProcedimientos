package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.TipoProcedimiento;

import java.util.List;

/** La "tarjeta" del procedimiento en el mazo de swipe. */
public record PerfilDTO(
        Long id,
        String codigo,
        String nombre,
        TipoProcedimiento tipo,
        String bio,
        int edadAnios,
        int minutosPorUso,
        String beneficio,
        String usoTexto,
        String padrinoNombre,
        boolean obligatorio,
        String baseObligatoriedad,
        List<String> areas) {
}
