package ar.gob.defensoria.tinder.repositorio;

import ar.gob.defensoria.tinder.dominio.ValorVoto;

/** Proyección agregada: cuántos votos de un valor tuvo un procedimiento en un área. */
public record ConteoVotos(Long procedimientoId, Long areaId, ValorVoto valor, long cantidad) {
}
