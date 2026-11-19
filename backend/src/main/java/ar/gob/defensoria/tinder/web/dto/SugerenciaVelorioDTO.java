package ar.gob.defensoria.tinder.web.dto;


public record SugerenciaVelorioDTO(
        Long procedimientoId,
        String codigo,
        String nombre,
        String causa,
        String epitafio,
        int minutosSemanalesPerdidos,
        String deudos,
        String duenoNombre,
        String proximoNumeroActa) {
}
