package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Acción del tablero de mejoras: toda objeción, acuerdo y acta genera una, con responsable y fecha. */
@Entity
@Table(name = "mejora")
public class Mejora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigenMejora origen;

    /** Id de la objeción (UUID), segunda cita, acta o casamiento que la originó. */
    @Column(name = "origen_id")
    private String origenId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "procedimiento_id")
    private Procedimiento procedimiento;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    @Column(name = "fecha_compromiso", nullable = false)
    private LocalDate fechaCompromiso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMejora estado = EstadoMejora.NUEVA;

    @Column(columnDefinition = "TEXT")
    private String respuesta;

    protected Mejora() {
    }

    public Mejora(String codigo, OrigenMejora origen, String origenId, Procedimiento procedimiento, String descripcion,
                  Usuario responsable, LocalDate fechaCompromiso) {
        this.codigo = codigo;
        this.origen = origen;
        this.origenId = origenId;
        this.procedimiento = procedimiento;
        this.descripcion = descripcion;
        this.responsable = responsable;
        this.fechaCompromiso = fechaCompromiso;
    }

    public boolean isCerrada() {
        return estado == EstadoMejora.RESUELTA || estado == EstadoMejora.DESCARTADA;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public OrigenMejora getOrigen() { return origen; }
    public String getOrigenId() { return origenId; }
    public Procedimiento getProcedimiento() { return procedimiento; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Usuario getResponsable() { return responsable; }
    public void setResponsable(Usuario responsable) { this.responsable = responsable; }
    public LocalDate getFechaCompromiso() { return fechaCompromiso; }
    public void setFechaCompromiso(LocalDate fechaCompromiso) { this.fechaCompromiso = fechaCompromiso; }
    public EstadoMejora getEstado() { return estado; }
    public void setEstado(EstadoMejora estado) { this.estado = estado; }
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
}
