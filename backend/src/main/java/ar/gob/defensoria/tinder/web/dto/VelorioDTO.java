package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** El acta de defunción. */
public record VelorioDTO(
        Long id,
        String numeroActa,
        Long procedimientoId,
        String codigo,
        String nombre,
        int version,
        LocalDate fechaNacimiento,
        LocalDate fecha,
        String causa,
        String epitafio,
        int minutosSemanalesPerdidos,
        String deudos,
        String herencia,
        String lecciones,
        Long reencarnacionId,
        String reencarnacionCodigo,
        String reencarnacionNombre,
        EstadoProcedimiento reencarnacionEstado,
        String sinReemplazoMotivo,
        Long duenoId,
        String duenoNombre,
        List<Firma> firmas,
        boolean completa) {

    /** rol: JUEZ_PAZ, DIRECCION o DUENO. */
    public record Firma(String rol, String etiqueta, Long usuarioId, String nombre, LocalDateTime fecha) {
    }
}
