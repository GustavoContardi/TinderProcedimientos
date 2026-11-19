package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Foto de la aprobación de un área en el momento del casamiento. */
@Embeddable
public class CasamientoResultado {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "area_id")
    private Area area;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal pct;

    protected CasamientoResultado() {
    }

    public CasamientoResultado(Area area, BigDecimal pct) {
        this.area = area;
        this.pct = pct;
    }

    public Area getArea() { return area; }
    public BigDecimal getPct() { return pct; }
}
