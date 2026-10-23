package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.TipoProcedimiento;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Set;

public record ProcedimientoRequest(
        @NotBlank(message = "El código es obligatorio (ej. PR-07)") @Size(max = 20) String codigo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 200) String nombre,
        @NotNull(message = "El tipo es obligatorio") TipoProcedimiento tipo,
        @NotBlank(message = "Sin bio no hay match: escribí algo") String bio,
        @NotNull(message = "Falta la fecha de creación") @PastOrPresent(message = "No puede haber nacido en el futuro") LocalDate fechaCreacion,
        @NotNull @Min(value = 0, message = "Los minutos por uso no pueden ser negativos") Integer minutosPorUso,
        @NotNull @Min(value = 0, message = "Los usos por semana no pueden ser negativos") Integer usosPorSemana,
        @NotBlank(message = "Contá qué beneficio da") String beneficio,
        boolean obligatorio,
        String baseObligatoriedad,
        Long autorId,
        String versionExpress,
        @NotEmpty(message = "El procedimiento tiene que pertenecer al menos a un área") Set<Long> areaIds) {
}
