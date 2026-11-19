package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.web.dto.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Conversión entidad → DTO, a mano (sin MapStruct). */
public final class Mapeador {

    private Mapeador() {
    }

    public static int edadAnios(LocalDate fechaCreacion) {
        return Math.max(0, Period.between(fechaCreacion, LocalDate.now()).getYears());
    }

    public static String nombre(Usuario u) {
        return u != null ? u.getNombre() : null;
    }

    public static Long id(Usuario u) {
        return u != null ? u.getId() : null;
    }

    /** 0,8750 → 88 */
    public static int pct(BigDecimal fraccion) {
        return fraccion.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public static List<AreaDTO> areas(Procedimiento p) {
        return p.getAreas().stream().map(AreaDTO::de).toList();
    }

    public static ProcedimientoDTO procedimiento(Procedimiento p) {
        return procedimiento(p, false);
    }

    public static ProcedimientoDTO procedimiento(Procedimiento p, boolean editablePorDueno) {
        Procedimiento r = p.getReemplazaA();
        return new ProcedimientoDTO(p.getId(), p.getCodigo(), p.getNombre(), p.getTipo(), p.getBio(),
                p.getFechaCreacion(), edadAnios(p.getFechaCreacion()), p.getMinutosPorUso(), p.getUsosPorSemana(),
                p.minutosSemanales(), p.getBeneficio(), p.isObligatorio(), p.getBaseObligatoriedad(),
                id(p.getAutor()), nombre(p.getAutor()), id(p.getDueno()), nombre(p.getDueno()), p.getNumeroVersion(),
                p.getVersionExpress(), p.getEstado(), r != null ? r.getId() : null, r != null ? r.getCodigo() : null,
                p.getSegundasCitasConsecutivas(), p.isRequiereReformulacion(), editablePorDueno, areas(p));
    }

    public static MejoraDTO mejora(Mejora m) {
        Procedimiento p = m.getProcedimiento();
        boolean vencida = !m.isCerrada() && m.getFechaCompromiso().isBefore(LocalDate.now());
        return new MejoraDTO(m.getId(), m.getCodigo(), m.getOrigen(), m.getOrigenId(), p.getId(), p.getCodigo(),
                p.getNombre(), m.getDescripcion(), m.getResponsable().getId(), m.getResponsable().getNombre(),
                m.getFechaCompromiso(), vencida, m.getEstado(), m.getRespuesta());
    }

    public static CasamientoDTO casamiento(Casamiento c, List<Objecion> objecionesTratadas) {
        Procedimiento p = c.getProcedimiento();
        return new CasamientoDTO(c.getId(), c.getNumero(), p.getId(), p.getCodigo(), p.getNombre(), p.isObligatorio(),
                c.getVersionProcedimiento(), p.getVersionExpress(), c.getFecha(), c.getVotos(), nombre(c.getPadrino()),
                nombre(c.getDueno()), c.getOficiante().getNombre(), c.isGrupal(), c.getCondiciones(),
                c.getInformeAniversario(), c.getFechaAniversario(),
                ChronoUnit.DAYS.between(LocalDate.now(), c.getFechaAniversario()), c.getEstado(),
                c.getAreas().stream().map(Area::getNombre).toList(),
                c.getTestigos().stream()
                        .map(t -> new CasamientoDTO.Testigo(t.getId(), t.getNombre(), t.getArea().getNombre())).toList(),
                c.getResultados().stream()
                        .sorted(Comparator.comparing(r -> r.getArea().getId()))
                        .map(r -> new CasamientoDTO.PctArea(r.getArea().getNombre(), pct(r.getPct()))).toList(),
                objecionesTratadas.stream()
                        .map(o -> new CasamientoDTO.ObjecionTratada(o.getCategoria(), o.getOrigen(), o.getFundada(),
                                o.getRespuesta())).toList());
    }

    public static VelorioDTO velorio(Velorio v) {
        Procedimiento p = v.getProcedimiento();
        Procedimiento r = v.getReencarnacion();
        List<VelorioDTO.Firma> firmas = new ArrayList<>();
        firmas.add(new VelorioDTO.Firma("JUEZ_PAZ", "Juez de paz", id(v.getFirmaJuezPor()), nombre(v.getFirmaJuezPor()),
                v.getFirmaJuezFecha()));
        firmas.add(new VelorioDTO.Firma("DIRECCION", "Dirección (padrino institucional)", id(v.getFirmaDireccionPor()),
                nombre(v.getFirmaDireccionPor()), v.getFirmaDireccionFecha()));
        firmas.add(new VelorioDTO.Firma("DUENO", "Dueño del procedimiento", id(v.getFirmaDuenoPor()),
                nombre(v.getFirmaDuenoPor()), v.getFirmaDuenoFecha()));
        return new VelorioDTO(v.getId(), v.getNumeroActa(), p.getId(), p.getCodigo(), p.getNombre(),
                p.getNumeroVersion(), p.getFechaCreacion(), v.getFecha(), v.getCausa(), v.getEpitafio(),
                v.getMinutosSemanalesPerdidos(), v.getDeudos(), v.getHerencia(), v.getLecciones(),
                r != null ? r.getId() : null, r != null ? r.getCodigo() : null, r != null ? r.getNombre() : null,
                r != null ? r.getEstado() : null, v.getSinReemplazoMotivo(), id(p.getDueno()), nombre(p.getDueno()),
                firmas, v.completa());
    }
}
