package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Resultado consolidado de un procedimiento en un ciclo. */
@Entity
@Table(name = "resultado_procedimiento")
public class ResultadoProcedimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ciclo_id")
    private CicloVotacion ciclo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "procedimiento_id")
    private Procedimiento procedimiento;

    @Column(name = "pct_global", nullable = false, precision = 5, scale = 4)
    private BigDecimal pctGlobal;

    @Column(nullable = false)
    private int votantes;

    @Column(nullable = false)
    private int habilitados;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Veredicto destino;

    @Column(name = "relacion_a_distancia", nullable = false)
    private boolean relacionADistancia;

    @Column(nullable = false)
    private boolean grupal;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    protected ResultadoProcedimiento() {
    }

    public ResultadoProcedimiento(CicloVotacion ciclo, Procedimiento procedimiento, BigDecimal pctGlobal, int votantes,
                                  int habilitados, Veredicto destino, boolean relacionADistancia, boolean grupal,
                                  String mensaje) {
        this.ciclo = ciclo;
        this.procedimiento = procedimiento;
        this.pctGlobal = pctGlobal;
        this.votantes = votantes;
        this.habilitados = habilitados;
        this.destino = destino;
        this.relacionADistancia = relacionADistancia;
        this.grupal = grupal;
        this.mensaje = mensaje;
    }

    public Long getId() { return id; }
    public CicloVotacion getCiclo() { return ciclo; }
    public Procedimiento getProcedimiento() { return procedimiento; }
    public BigDecimal getPctGlobal() { return pctGlobal; }
    public int getVotantes() { return votantes; }
    public int getHabilitados() { return habilitados; }
    public Veredicto getDestino() { return destino; }
    public boolean isRelacionADistancia() { return relacionADistancia; }
    public boolean isGrupal() { return grupal; }
    public String getMensaje() { return mensaje; }
}
