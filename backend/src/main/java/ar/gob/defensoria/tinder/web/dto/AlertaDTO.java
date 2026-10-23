package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.Alerta;
import ar.gob.defensoria.tinder.dominio.TipoAlerta;

import java.time.LocalDateTime;

public record AlertaDTO(Long id, Long procedimientoId, String codigo, String nombre, String cicloNombre,
                        TipoAlerta tipo, String mensaje, boolean revisada, LocalDateTime createdAt) {

    public static AlertaDTO de(Alerta a) {
        return new AlertaDTO(a.getId(), a.getProcedimiento().getId(), a.getProcedimiento().getCodigo(),
                a.getProcedimiento().getNombre(), a.getCiclo().getNombre(), a.getTipo(), a.getMensaje(),
                a.isRevisada(), a.getCreatedAt());
    }
}
