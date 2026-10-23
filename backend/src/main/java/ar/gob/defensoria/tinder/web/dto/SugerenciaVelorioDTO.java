package ar.gob.defensoria.tinder.web.dto;

public record SugerenciaVelorioDTO(
        Long procedimientoId,
        String codigo,
        String nombre,
        String causaMuerte,
        String epitafio,
        int minutosSemanalesPerdidos,
        String deudos) {
}
