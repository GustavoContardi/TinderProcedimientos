package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "casamiento")
public class Casamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "procedimiento_id")
    private Procedimiento procedimiento;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String votos;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "padrino_id")
    private Usuario padrino;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "oficiante_id")
    private Usuario oficiante;

    @Column(nullable = false)
    private boolean grupal;

    @Column(name = "fecha_aniversario", nullable = false)
    private LocalDate fechaAniversario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCasamiento estado = EstadoCasamiento.VIGENTE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "casamiento_area",
            joinColumns = @JoinColumn(name = "casamiento_id"),
            inverseJoinColumns = @JoinColumn(name = "area_id"))
    @OrderBy("id")
    private Set<Area> areas = new LinkedHashSet<>();

    protected Casamiento() {
    }

    public Casamiento(Procedimiento procedimiento, LocalDate fecha, String votos, Usuario padrino,
                      Usuario oficiante, Set<Area> areas, LocalDate fechaAniversario) {
        this.procedimiento = procedimiento;
        this.fecha = fecha;
        this.votos = votos;
        this.padrino = padrino;
        this.oficiante = oficiante;
        this.areas = new LinkedHashSet<>(areas);
        this.grupal = areas.size() >= 2;
        this.fechaAniversario = fechaAniversario;
    }

    public Long getId() { return id; }
    public Procedimiento getProcedimiento() { return procedimiento; }
    public LocalDate getFecha() { return fecha; }
    public String getVotos() { return votos; }
    public Usuario getPadrino() { return padrino; }
    public Usuario getOficiante() { return oficiante; }
    public boolean isGrupal() { return grupal; }
    public LocalDate getFechaAniversario() { return fechaAniversario; }
    public EstadoCasamiento getEstado() { return estado; }
    public void setEstado(EstadoCasamiento estado) { this.estado = estado; }
    public Set<Area> getAreas() { return areas; }
}
