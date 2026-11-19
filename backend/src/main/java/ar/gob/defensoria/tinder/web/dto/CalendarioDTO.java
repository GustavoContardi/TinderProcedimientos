package ar.gob.defensoria.tinder.web.dto;

import java.util.List;

/** Checklist calculado del trimestre para el juez de paz. */
public record CalendarioDTO(CicloDTO ciclo, List<Item> items) {

    public record Item(String semana, String titulo, String detalle, boolean hecho) {
    }
}
