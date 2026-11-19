package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Objeción anónima de la minoría. A propósito NO tiene autor ni fecha/hora de creación:
 * la de origen VOTO se inserta en la misma transacción que el voto, sin nada que las una.
 * El id es un UUID aleatorio para que ni el orden de inserción delate a nadie.
 * {@code evaluadaPorId} es el juez de paz que la evaluó, nunca quien la escribió.
 */
@Entity
@Table(name = "objecion")
public class Objecion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "procedimiento_id", nullable = false)
    private Long procedimientoId;

    @Column(name = "ciclo_id")
    private Long cicloId;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigenObjecion origen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaObjecion categoria;

    @Column(columnDefinition = "TEXT")
    private String texto;

    private Boolean fundada;

    @Column(columnDefinition = "TEXT")
    private String respuesta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoObjecion estado = EstadoObjecion.PENDIENTE;

    @Column(name = "evaluada_por")
    private Long evaluadaPorId;

    protected Objecion() {
    }

    public Objecion(Long procedimientoId, Long cicloId, Long areaId, OrigenObjecion origen,
                    CategoriaObjecion categoria, String texto) {
        this.procedimientoId = procedimientoId;
        this.cicloId = cicloId;
        this.areaId = areaId;
        this.origen = origen;
        this.categoria = categoria;
        this.texto = texto;
    }

    /** Fundada sin respuesta queda EVALUADA; con respuesta, RESPONDIDA. */
    public void evaluar(boolean fundada, String respuesta, Long juezId) {
        this.fundada = fundada;
        this.respuesta = respuesta;
        this.evaluadaPorId = juezId;
        this.estado = respuesta == null ? EstadoObjecion.EVALUADA : EstadoObjecion.RESPONDIDA;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Long getProcedimientoId() { return procedimientoId; }
    public Long getCicloId() { return cicloId; }
    public Long getAreaId() { return areaId; }
    public OrigenObjecion getOrigen() { return origen; }
    public CategoriaObjecion getCategoria() { return categoria; }
    public String getTexto() { return texto; }
    public Boolean getFundada() { return fundada; }
    public String getRespuesta() { return respuesta; }
    public EstadoObjecion getEstado() { return estado; }
    public Long getEvaluadaPorId() { return evaluadaPorId; }

    @Override
    public String toString() {
        return "Objecion[procedimiento=" + procedimientoId + ", categoria=" + categoria + "]";
    }
}
