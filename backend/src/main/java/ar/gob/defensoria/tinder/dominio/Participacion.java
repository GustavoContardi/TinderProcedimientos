package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;

/**
 * Registra QUIÉN votó un procedimiento en un ciclo (para impedir el doble voto),
 * pero NUNCA qué votó. No tiene relación alguna con {@link Voto}.
 */
@Entity
@Table(name = "participacion")
public class Participacion implements Persistable<Participacion.Id> {

    @EmbeddedId
    private Id id;

    /** Siempre se inserta (persist), nunca se hace merge: así un doble voto choca contra la PK. */
    @Transient
    private boolean nueva = true;

    @PostLoad
    @PostPersist
    void marcarExistente() {
        nueva = false;
    }

    @Override
    public boolean isNew() {
        return nueva;
    }

    protected Participacion() {
    }

    public Participacion(Long usuarioId, Long procedimientoId, Long cicloId) {
        this.id = new Id(usuarioId, procedimientoId, cicloId);
    }

    @Override
    public Id getId() { return id; }

    @Embeddable
    public record Id(
            @Column(name = "usuario_id") Long usuarioId,
            @Column(name = "procedimiento_id") Long procedimientoId,
            @Column(name = "ciclo_id") Long cicloId) implements Serializable {
    }
}
