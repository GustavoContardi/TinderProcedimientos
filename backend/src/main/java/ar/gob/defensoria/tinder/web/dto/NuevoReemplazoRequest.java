package ar.gob.defensoria.tinder.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NuevoReemplazoRequest(
        @NotBlank(message = "El reemplazo necesita un código") @Size(max = 20) String codigo,
        @NotBlank(message = "El reemplazo necesita un nombre") @Size(max = 200) String nombre) {
}
