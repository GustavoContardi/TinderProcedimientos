package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "ciclo_votacion")
public class CicloVotacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCiclo estado = EstadoCiclo.ABIERTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCiclo tipo = TipoCiclo.TRIMESTRAL;

    /** Solo para los ciclos RAPIDA: el único procedimiento que se vota. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "procedimiento_id")
    private Procedimiento procedimiento;

    protected CicloVotacion() {
    }

    public CicloVotacion(String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    /** Votación rápida de un solo procedimiento. */
    public static CicloVotacion rapida(String nombre, Procedimiento p, LocalDate inicio, LocalDate fin) {
        CicloVotacion c = new CicloVotacion(nombre, inicio, fin);
        c.tipo = TipoCiclo.RAPIDA;
        c.procedimiento = p;
        return c;
    }

    public boolean esRapida() {
        return tipo == TipoCiclo.RAPIDA;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public EstadoCiclo getEstado() { return estado; }
    public void cerrar() { this.estado = EstadoCiclo.CERRADO; }
    public TipoCiclo getTipo() { return tipo; }
    public Procedimiento getProcedimiento() { return procedimiento; }
}
