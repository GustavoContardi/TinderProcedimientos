package ar.gob.defensoria.tinder.web.dto;

import jakarta.validation.constraints.*;

/** Lo que el dueño puede cambiar tras una segunda cita con AJUSTAR_Y_REVOTAR (cada cambio sube la versión). */
public record ContenidoRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 200) String nombre,
        @NotBlank(message = "La bio es obligatoria") String bio,
        @NotNull @Min(value = 0, message = "Los minutos por uso no pueden ser negativos") Integer minutosPorUso,
        @NotNull @Min(value = 0, message = "Los usos por semana no pueden ser negativos") Integer usosPorSemana,
        @NotBlank(message = "Contá qué beneficio da") String beneficio,
        String versionExpress) {
}
