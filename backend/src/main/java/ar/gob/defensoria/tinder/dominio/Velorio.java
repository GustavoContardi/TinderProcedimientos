package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Acta de defunción. Nunca se borra nada: el procedimiento queda como documento obsoleto (ISO 9001 7.5.3).
 * Deja trazabilidad: causa, herencia, lecciones, reencarnación y tres firmas (juez de paz, Dirección y dueño).
 */
@Entity
@Table(name = "velorio")
public class Velorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ej. "004/2026", secuencial por año. */
    @Column(name = "numero_acta", nullable = false, unique = true)
    private String numeroActa;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "procedimiento_id", unique = true)
    private Procedimiento procedimiento;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String causa;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String epitafio;

    @Column(name = "minutos_semanales_perdidos", nullable = false)
    private int minutosSemanalesPerdidos;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String deudos = "nadie";

    /** Lo que se rescata del difunto y pasa a otro lado. */
    @Column(columnDefinition = "TEXT")
    private String herencia;

    @Column(columnDefinition = "TEXT")
    private String lecciones;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reencarnacion_id")
    private Procedimiento reencarnacion;

    @Column(name = "sin_reemplazo_motivo", columnDefinition = "TEXT")
    private String sinReemplazoMotivo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "firma_juez_por")
    private Usuario firmaJuezPor;

    @Column(name = "firma_juez_fecha")
    private LocalDateTime firmaJuezFecha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "firma_direccion_por")
    private Usuario firmaDireccionPor;

    @Column(name = "firma_direccion_fecha")
    private LocalDateTime firmaDireccionFecha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "firma_dueno_por")
    private Usuario firmaDuenoPor;

    @Column(name = "firma_dueno_fecha")
    private LocalDateTime firmaDuenoFecha;

    protected Velorio() {
    }

    public Velorio(String numeroActa, Procedimiento procedimiento, LocalDate fecha, String causa, String epitafio,
                   int minutosSemanalesPerdidos, String deudos, String herencia, String lecciones,
                   Procedimiento reencarnacion, String sinReemplazoMotivo) {
        this.numeroActa = numeroActa;
        this.procedimiento = procedimiento;
        this.fecha = fecha;
        this.causa = causa;
        this.epitafio = epitafio;
        this.minutosSemanalesPerdidos = minutosSemanalesPerdidos;
        this.deudos = deudos;
        this.herencia = herencia;
        this.lecciones = lecciones;
        this.reencarnacion = reencarnacion;
        this.sinReemplazoMotivo = sinReemplazoMotivo;
    }

    public void firmarComoJuez(Usuario u) {
        firmaJuezPor = u;
        firmaJuezFecha = LocalDateTime.now();
    }

    public void firmarComoDireccion(Usuario u) {
        firmaDireccionPor = u;
        firmaDireccionFecha = LocalDateTime.now();
    }

    public void firmarComoDueno(Usuario u) {
        firmaDuenoPor = u;
        firmaDuenoFecha = LocalDateTime.now();
    }

    public boolean firmoAlguna(Long usuarioId) {
        return esDe(firmaJuezPor, usuarioId) || esDe(firmaDireccionPor, usuarioId) || esDe(firmaDuenoPor, usuarioId);
    }

    public boolean completa() {
        return firmaJuezPor != null && firmaDireccionPor != null && firmaDuenoPor != null;
    }

    private static boolean esDe(Usuario u, Long id) {
        return u != null && u.getId().equals(id);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroActa() { return numeroActa; }
    public Procedimiento getProcedimiento() { return procedimiento; }
    public LocalDate getFecha() { return fecha; }
    public String getCausa() { return causa; }
    public String getEpitafio() { return epitafio; }
    public int getMinutosSemanalesPerdidos() { return minutosSemanalesPerdidos; }
    public String getDeudos() { return deudos; }
    public String getHerencia() { return herencia; }
    public String getLecciones() { return lecciones; }
    public Procedimiento getReencarnacion() { return reencarnacion; }
    public String getSinReemplazoMotivo() { return sinReemplazoMotivo; }
    public Usuario getFirmaJuezPor() { return firmaJuezPor; }
    public LocalDateTime getFirmaJuezFecha() { return firmaJuezFecha; }
    public Usuario getFirmaDireccionPor() { return firmaDireccionPor; }
    public LocalDateTime getFirmaDireccionFecha() { return firmaDireccionFecha; }
    public Usuario getFirmaDuenoPor() { return firmaDuenoPor; }
    public LocalDateTime getFirmaDuenoFecha() { return firmaDuenoFecha; }
}
