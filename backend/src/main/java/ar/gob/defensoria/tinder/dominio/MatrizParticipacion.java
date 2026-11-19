package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDate;

/** "Este usuario usa o ejecuta este procedimiento": solo vota lo que está en su matriz. */
@Entity
@Table(name = "matriz_participacion")
public class MatrizParticipacion {

    @EmbeddedId
    private Id id;

    @Column(nullable = false)
    private LocalDate alta;

    protected MatrizParticipacion() {
    }

    public MatrizParticipacion(Long usuarioId, Long procedimientoId, LocalDate alta) {
        this.id = new Id(usuarioId, procedimientoId);
        this.alta = alta;
    }

    public Id getId() { return id; }
    public LocalDate getAlta() { return alta; }

    @Embeddable
    public record Id(
            @Column(name = "usuario_id") Long usuarioId,
            @Column(name = "procedimiento_id") Long procedimientoId) implements Serializable {
    }
}
