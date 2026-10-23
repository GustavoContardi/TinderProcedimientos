package ar.gob.defensoria.tinder.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

public record CasamientoRequest(
        @NotNull(message = "Falta el procedimiento") Long procedimientoId,
        LocalDate fecha,
        @NotBlank(message = "Sin votos no hay casamiento") String votos,
        @NotBlank(message = "Falta el acuerdo prenupcial (versión express)") String versionExpress,
        @NotNull(message = "Falta el oficiante (de la Dirección)") Long oficianteId,
        Long padrinoId,
        Set<Long> areaIds) {
}
