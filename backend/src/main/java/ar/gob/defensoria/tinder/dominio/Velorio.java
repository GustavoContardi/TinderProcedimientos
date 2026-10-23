package ar.gob.defensoria.tinder.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Acta de defunción. Nunca se borra nada: el procedimiento queda como documento obsoleto (ISO 9001 7.5.3). */
@Entity
@Table(name = "velorio")
public class Velorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "procedimiento_id", unique = true)
    private Procedimiento procedimiento;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "causa_muerte", nullable = false, columnDefinition = "TEXT")
    private String causaMuerte;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String epitafio;

    @Column(name = "minutos_semanales_perdidos", nullable = false)
    private int minutosSemanalesPerdidos;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String deudos = "nadie";

    @Column(name = "acta_firmada", nullable = false)
    private boolean actaFirmada;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "firmado_por")
    private Usuario firmadoPor;

    @Column(name = "fecha_firma")
    private LocalDateTime fechaFirma;

    protected Velorio() {
    }

    public Velorio(Procedimiento procedimiento, LocalDate fecha, String causaMuerte, String epitafio,
                   int minutosSemanalesPerdidos, String deudos) {
        this.procedimiento = procedimiento;
        this.fecha = fecha;
        this.causaMuerte = causaMuerte;
        this.epitafio = epitafio;
        this.minutosSemanalesPerdidos = minutosSemanalesPerdidos;
        this.deudos = deudos;
    }

    public void firmar(Usuario director) {
        this.actaFirmada = true;
        this.firmadoPor = director;
        this.fechaFirma = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Procedimiento getProcedimiento() { return procedimiento; }
    public LocalDate getFecha() { return fecha; }
    public String getCausaMuerte() { return causaMuerte; }
    public String getEpitafio() { return epitafio; }
    public int getMinutosSemanalesPerdidos() { return minutosSemanalesPerdidos; }
    public String getDeudos() { return deudos; }
    public boolean isActaFirmada() { return actaFirmada; }
    public Usuario getFirmadoPor() { return firmadoPor; }
    public LocalDateTime getFechaFirma() { return fechaFirma; }
}
