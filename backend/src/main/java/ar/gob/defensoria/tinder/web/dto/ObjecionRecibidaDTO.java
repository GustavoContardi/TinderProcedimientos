package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.OrigenObjecion;

public record ObjecionRecibidaDTO(OrigenObjecion origen, String mensaje) {
}
