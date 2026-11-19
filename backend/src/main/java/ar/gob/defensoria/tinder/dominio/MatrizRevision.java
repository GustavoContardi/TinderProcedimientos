package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Bitácora: cada vez que el juez de paz guarda la matriz queda un renglón (lo usa el calendario). */
@Entity
@Table(name = "matriz_revision")
public class MatrizRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private int altas;

    @Column(nullable = false)
    private int bajas;

    protected MatrizRevision() {
    }

    public MatrizRevision(LocalDate fecha, Long usuarioId, int altas, int bajas) {
        this.fecha = fecha;
        this.usuarioId = usuarioId;
        this.altas = altas;
        this.bajas = bajas;
    }

    public Long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public Long getUsuarioId() { return usuarioId; }
    public int getAltas() { return altas; }
    public int getBajas() { return bajas; }
}
