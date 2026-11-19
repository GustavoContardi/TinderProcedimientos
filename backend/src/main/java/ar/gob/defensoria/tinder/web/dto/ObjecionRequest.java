package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.CategoriaObjecion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Objeción anónima: solo categoría y texto, nunca quién la escribe. */
public record ObjecionRequest(
        @NotNull(message = "Elegí una categoría para la objeción") CategoriaObjecion categoria,
        @Size(max = 1000, message = "La objeción no puede superar los 1000 caracteres") String texto) {
}
