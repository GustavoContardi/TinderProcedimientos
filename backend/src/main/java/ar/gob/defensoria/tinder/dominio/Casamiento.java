package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** La libreta de casamiento: aprobación formal con compromiso público. */
@Entity
@Table(name = "casamiento")
public class Casamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ej. "L-012/2026". */
    @Column(nullable = false, unique = true)
    private String numero;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "procedimiento_id")
    private Procedimiento procedimiento;

    @Column(name = "version_procedimiento", nullable = false)
    private int versionProcedimiento;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String votos;

    /** Padrino = autor del procedimiento. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "padrino_id")
    private Usuario padrino;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dueno_id")
    private Usuario dueno;

    /** Oficia el juez de paz. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "oficiante_id")
    private Usuario oficiante;

    @Column(nullable = false)
    private boolean grupal;

    @Column(columnDefinition = "TEXT")
    private String condiciones;

    @Column(name = "informe_aniversario", columnDefinition = "TEXT")
    private String informeAniversario;

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

    /** Usuarios de OTRAS áreas que reciben el trabajo. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "casamiento_testigo",
            joinColumns = @JoinColumn(name = "casamiento_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id"))
    @OrderBy("nombre")
    private Set<Usuario> testigos = new LinkedHashSet<>();

    /** Foto del % de aprobación por área al momento de casarse. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "casamiento_resultado", joinColumns = @JoinColumn(name = "casamiento_id"))
    private List<CasamientoResultado> resultados = new ArrayList<>();

    protected Casamiento() {
    }

    public Casamiento(String numero, Procedimiento procedimiento, LocalDate fecha, String votos, Usuario padrino,
                      Usuario dueno, Usuario oficiante, Set<Area> areas, Set<Usuario> testigos,
                      List<CasamientoResultado> resultados, String condiciones, LocalDate fechaAniversario) {
        this.numero = numero;
        this.procedimiento = procedimiento;
        this.versionProcedimiento = procedimiento.getNumeroVersion();
        this.fecha = fecha;
        this.votos = votos;
        this.padrino = padrino;
        this.dueno = dueno;
        this.oficiante = oficiante;
        this.areas = new LinkedHashSet<>(areas);
        this.grupal = areas.size() >= 2;
        this.testigos = new LinkedHashSet<>(testigos);
        this.resultados = new ArrayList<>(resultados);
        this.condiciones = condiciones;
        this.fechaAniversario = fechaAniversario;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public Procedimiento getProcedimiento() { return procedimiento; }
    public int getVersionProcedimiento() { return versionProcedimiento; }
    public LocalDate getFecha() { return fecha; }
    public String getVotos() { return votos; }
    public Usuario getPadrino() { return padrino; }
    public Usuario getDueno() { return dueno; }
    public Usuario getOficiante() { return oficiante; }
    public boolean isGrupal() { return grupal; }
    public String getCondiciones() { return condiciones; }
    public String getInformeAniversario() { return informeAniversario; }
    public void setInformeAniversario(String informeAniversario) { this.informeAniversario = informeAniversario; }
    public LocalDate getFechaAniversario() { return fechaAniversario; }
    public EstadoCasamiento getEstado() { return estado; }
    public void setEstado(EstadoCasamiento estado) { this.estado = estado; }
    public Set<Area> getAreas() { return areas; }
    public Set<Usuario> getTestigos() { return testigos; }
    public List<CasamientoResultado> getResultados() { return resultados; }
}
