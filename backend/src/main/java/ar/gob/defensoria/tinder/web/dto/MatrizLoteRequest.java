package ar.gob.defensoria.tinder.web.dto;

import jakarta.validation.Valid;

import java.util.List;

/** Guardado por lote de la matriz. */
public record MatrizLoteRequest(@Valid List<CeldaDTO> altas, @Valid List<CeldaDTO> bajas) {
}
