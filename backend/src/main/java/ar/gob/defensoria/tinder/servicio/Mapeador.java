package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.web.dto.*;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Conversión entidad → DTO, a mano (sin MapStruct). */
public final class Mapeador {

    private Mapeador() {
    }

    public static int edadAnios(LocalDate fechaCreacion) {
        return Math.max(0, Period.between(fechaCreacion, LocalDate.now()).getYears());
    }

    public static String padrino(Usuario u) {
        return u != null ? u.getNombre() : null;
    }

    public static List<AreaDTO> areas(Procedimiento p) {
        return p.getAreas().stream().map(AreaDTO::de).toList();
    }

    public static ProcedimientoDTO procedimiento(Procedimiento p) {
        return new ProcedimientoDTO(p.getId(), p.getCodigo(), p.getNombre(), p.getTipo(), p.getBio(),
                p.getFechaCreacion(), edadAnios(p.getFechaCreacion()), p.getMinutosPorUso(), p.getUsosPorSemana(),
                p.minutosSemanales(), p.getBeneficio(), p.isObligatorio(), p.getBaseObligatoriedad(),
                p.getAutor() != null ? p.getAutor().getId() : null, padrino(p.getAutor()), p.getVersionExpress(),
                p.getEstado(), areas(p));
    }

    public static CasamientoDTO casamiento(Casamiento c) {
        Procedimiento p = c.getProcedimiento();
        return new CasamientoDTO(c.getId(), p.getId(), p.getCodigo(), p.getNombre(), p.isObligatorio(),
                p.getVersionExpress(), c.getFecha(), c.getVotos(), padrino(c.getPadrino()),
                c.getOficiante().getNombre(), c.isGrupal(), c.getFechaAniversario(),
                ChronoUnit.DAYS.between(LocalDate.now(), c.getFechaAniversario()), c.getEstado(),
                c.getAreas().stream().map(Area::getNombre).toList());
    }

    public static VelorioDTO velorio(Velorio v) {
        Procedimiento p = v.getProcedimiento();
        return new VelorioDTO(v.getId(), p.getId(), p.getCodigo(), p.getNombre(), p.getFechaCreacion(), v.getFecha(),
                v.getCausaMuerte(), v.getEpitafio(), v.getMinutosSemanalesPerdidos(), v.getDeudos(),
                v.isActaFirmada(), padrino(v.getFirmadoPor()), v.getFechaFirma());
    }
}
