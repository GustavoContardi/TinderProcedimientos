package ar.gob.defensoria.tinder.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VelorioDTO(
        Long id,
        Long procedimientoId,
        String codigo,
        String nombre,
        LocalDate fechaNacimiento,
        LocalDate fecha,
        String causaMuerte,
        String epitafio,
        int minutosSemanalesPerdidos,
        String deudos,
        boolean actaFirmada,
        String firmadoPorNombre,
        LocalDateTime fechaFirma) {
}
