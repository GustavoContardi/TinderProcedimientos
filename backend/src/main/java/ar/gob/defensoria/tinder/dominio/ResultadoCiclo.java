package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "resultado_ciclo")
public class ResultadoCiclo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ciclo_id")
    private CicloVotacion ciclo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "procedimiento_id")
    private Procedimiento procedimiento;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "area_id")
    private Area area;

    @Column(nullable = false)
    private int likes;

    @Column(nullable = false)
    private int nopes;

    @Column(nullable = false)
    private int total;

    @Column(name = "pct_like", nullable = false, precision = 5, scale = 4)
    private BigDecimal pctLike;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Veredicto veredicto;

    protected ResultadoCiclo() {
    }

    public ResultadoCiclo(CicloVotacion ciclo, Procedimiento procedimiento, Area area,
                          int likes, int nopes, BigDecimal pctLike, Veredicto veredicto) {
        this.ciclo = ciclo;
        this.procedimiento = procedimiento;
        this.area = area;
        this.likes = likes;
        this.nopes = nopes;
        this.total = likes + nopes;
        this.pctLike = pctLike;
        this.veredicto = veredicto;
    }

    public Long getId() { return id; }
    public CicloVotacion getCiclo() { return ciclo; }
    public Procedimiento getProcedimiento() { return procedimiento; }
    public Area getArea() { return area; }
    public int getLikes() { return likes; }
    public int getNopes() { return nopes; }
    public int getTotal() { return total; }
    public BigDecimal getPctLike() { return pctLike; }
    public Veredicto getVeredicto() { return veredicto; }
}
