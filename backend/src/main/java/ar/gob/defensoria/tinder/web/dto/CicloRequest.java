package ar.gob.defensoria.tinder.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CicloRequest(
        @NotBlank(message = "El ciclo necesita un nombre, por ejemplo 2027-T1") @Size(max = 20) String nombre,
        @NotNull(message = "Falta la fecha de inicio") LocalDate fechaInicio,
        @NotNull(message = "Falta la fecha de fin") LocalDate fechaFin) {
}
