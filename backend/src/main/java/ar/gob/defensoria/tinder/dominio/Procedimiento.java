package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "procedimiento")
public class Procedimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoProcedimiento tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bio;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "minutos_por_uso", nullable = false)
    private int minutosPorUso;

    @Column(name = "usos_por_semana", nullable = false)
    private int usosPorSemana;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String beneficio;

    @Column(nullable = false)
    private boolean obligatorio;

    @Column(name = "base_obligatoriedad", columnDefinition = "TEXT")
    private String baseObligatoriedad;

    /** El autor es el "padrino" del procedimiento. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    /** Acuerdo prenupcial: versión de una página para urgencias. */
    @Column(name = "version_express", columnDefinition = "TEXT")
    private String versionExpress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoProcedimiento estado = EstadoProcedimiento.EN_TINDER;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "procedimiento_area",
            joinColumns = @JoinColumn(name = "procedimiento_id"),
            inverseJoinColumns = @JoinColumn(name = "area_id"))
    @OrderBy("id")
    private Set<Area> areas = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public int minutosSemanales() {
        return minutosPorUso * usosPorSemana;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoProcedimiento getTipo() { return tipo; }
    public void setTipo(TipoProcedimiento tipo) { this.tipo = tipo; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public int getMinutosPorUso() { return minutosPorUso; }
    public void setMinutosPorUso(int minutosPorUso) { this.minutosPorUso = minutosPorUso; }
    public int getUsosPorSemana() { return usosPorSemana; }
    public void setUsosPorSemana(int usosPorSemana) { this.usosPorSemana = usosPorSemana; }
    public String getBeneficio() { return beneficio; }
    public void setBeneficio(String beneficio) { this.beneficio = beneficio; }
    public boolean isObligatorio() { return obligatorio; }
    public void setObligatorio(boolean obligatorio) { this.obligatorio = obligatorio; }
    public String getBaseObligatoriedad() { return baseObligatoriedad; }
    public void setBaseObligatoriedad(String baseObligatoriedad) { this.baseObligatoriedad = baseObligatoriedad; }
    public Usuario getAutor() { return autor; }
    public void setAutor(Usuario autor) { this.autor = autor; }
    public String getVersionExpress() { return versionExpress; }
    public void setVersionExpress(String versionExpress) { this.versionExpress = versionExpress; }
    public EstadoProcedimiento getEstado() { return estado; }

    /** Solo la {@link ar.gob.defensoria.tinder.servicio.MaquinaEstados} debería llamar a este método. */
    public void setEstado(EstadoProcedimiento estado) { this.estado = estado; }

    public Set<Area> getAreas() { return areas; }
    public void setAreas(Set<Area> areas) { this.areas = areas; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
