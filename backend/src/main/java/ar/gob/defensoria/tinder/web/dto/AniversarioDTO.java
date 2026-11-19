package ar.gob.defensoria.tinder.web.dto;

import java.util.List;

/** Informe de aniversario: lo que el juez mira antes de renovar, citar o divorciar. */
public record AniversarioDTO(CasamientoDTO casamiento, long objecionesAbiertas, List<MejoraDTO> mejorasAbiertas) {
}
