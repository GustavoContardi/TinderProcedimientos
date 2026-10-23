package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Registra QUÉ se votó. A propósito NO tiene usuario ni fecha/hora:
 * es imposible reconstruir quién votó qué. El id es un UUID aleatorio (generado por Hibernate)
 * para que ni siquiera el orden de inserción delate a nadie.
 */
@Entity
@Table(name = "voto")
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "procedimiento_id", nullable = false)
    private Long procedimientoId;

    @Column(name = "ciclo_id", nullable = false)
    private Long cicloId;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValorVoto valor;

    protected Voto() {
    }

    public Voto(Long procedimientoId, Long cicloId, Long areaId, ValorVoto valor) {
        this.procedimientoId = procedimientoId;
        this.cicloId = cicloId;
        this.areaId = areaId;
        this.valor = valor;
    }

    public UUID getId() { return id; }
    public Long getProcedimientoId() { return procedimientoId; }
    public Long getCicloId() { return cicloId; }
    public Long getAreaId() { return areaId; }
    public ValorVoto getValor() { return valor; }

    @Override
    public String toString() {
        return "Voto[procedimiento=" + procedimientoId + ", ciclo=" + cicloId + ", area=" + areaId + "]";
    }
}
