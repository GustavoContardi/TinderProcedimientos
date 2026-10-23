package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Aviso para que Calidad revise un procedimiento después de cerrar un ciclo. */
@Entity
@Table(name = "alerta")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "procedimiento_id")
    private Procedimiento procedimiento;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ciclo_id")
    private CicloVotacion ciclo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAlerta tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false)
    private boolean revisada;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Alerta() {
    }

    public Alerta(Procedimiento procedimiento, CicloVotacion ciclo, TipoAlerta tipo, String mensaje) {
        this.procedimiento = procedimiento;
        this.ciclo = ciclo;
        this.tipo = tipo;
        this.mensaje = mensaje;
    }

    public Long getId() { return id; }
    public Procedimiento getProcedimiento() { return procedimiento; }
    public CicloVotacion getCiclo() { return ciclo; }
    public TipoAlerta getTipo() { return tipo; }
    public String getMensaje() { return mensaje; }
    public boolean isRevisada() { return revisada; }
    public void marcarRevisada() { this.revisada = true; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
