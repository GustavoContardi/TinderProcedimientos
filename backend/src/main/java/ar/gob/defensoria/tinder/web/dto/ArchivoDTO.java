package ar.gob.defensoria.tinder.web.dto;

import java.time.LocalDate;

/** Procedimiento velado (obsoleto) con su acta. Solo lectura. */
public record ArchivoDTO(ProcedimientoDTO procedimiento, Long velorioId, String numeroActa, LocalDate fechaVelorio,
                         String reemplazoCodigo, String reemplazoNombre) {
}
