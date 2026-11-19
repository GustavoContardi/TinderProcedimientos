package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Reunión formal de 30 minutos con minuta y decisión. Ninguna decisión cambia el destino sin votación anónima. */
@Entity
@Table(name = "segunda_cita")
public class SegundaCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "procedimiento_id")
    private Procedimiento procedimiento;

    /** Ciclo que la originó (null si nació de una objeción fundada en la boda o el aniversario). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ciclo_id")
    private CicloVotacion ciclo;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDate fechaProgramada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSegundaCita estado = EstadoSegundaCita.CONVOCADA;

    @Column(columnDefinition = "TEXT")
    private String minuta;

    @Enumerated(EnumType.STRING)
    private DecisionSegundaCita decision;

    @Column(columnDefinition = "TEXT")
    private String condiciones;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ciclo_rapido_id")
    private CicloVotacion cicloRapido;

    protected SegundaCita() {
    }

    public SegundaCita(Procedimiento procedimiento, CicloVotacion ciclo, LocalDate fechaProgramada) {
        this.procedimiento = procedimiento;
        this.ciclo = ciclo;
        this.fechaProgramada = fechaProgramada;
    }

    public void realizar(String minuta, DecisionSegundaCita decision, String condiciones) {
        this.minuta = minuta;
        this.decision = decision;
        this.condiciones = condiciones;
        this.estado = EstadoSegundaCita.REALIZADA;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Procedimiento getProcedimiento() { return procedimiento; }
    public CicloVotacion getCiclo() { return ciclo; }
    public LocalDate getFechaProgramada() { return fechaProgramada; }
    public EstadoSegundaCita getEstado() { return estado; }
    public String getMinuta() { return minuta; }
    public DecisionSegundaCita getDecision() { return decision; }
    public String getCondiciones() { return condiciones; }
    public CicloVotacion getCicloRapido() { return cicloRapido; }
    public void setCicloRapido(CicloVotacion cicloRapido) { this.cicloRapido = cicloRapido; }
}
