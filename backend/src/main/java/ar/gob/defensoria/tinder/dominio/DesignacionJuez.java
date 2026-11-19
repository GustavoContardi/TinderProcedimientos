package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Historial de la rotación del juez de paz. Vigente = hasta es null (uno por área). */
@Entity
@Table(name = "designacion_juez")
public class DesignacionJuez {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "area_id")
    private Area area;

    @Column(nullable = false)
    private LocalDate desde;

    private LocalDate hasta;

    protected DesignacionJuez() {
    }

    public DesignacionJuez(Usuario usuario, Area area, LocalDate desde) {
        this.usuario = usuario;
        this.area = area;
        this.desde = desde;
    }

    public void cerrar(LocalDate hasta) {
        this.hasta = hasta.isBefore(desde) ? desde : hasta;
    }

    public boolean isVigente() { return hasta == null; }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Area getArea() { return area; }
    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
}
