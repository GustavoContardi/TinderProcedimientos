package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.TinderProperties;
import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

final class Fixtures {

    static final Area OPERACIONES = new Area(1L, "Operaciones y Mesa de Ayuda");
    static final Area DESARROLLO = new Area(2L, "Desarrollo");
    static final Area INFRA = new Area(3L, "Infraestructura");
    static final Area ATENCION = new Area(4L, "Atención al Público");

    private Fixtures() {
    }

    static TinderProperties props() {
        return TinderProperties.porDefecto();
    }

    static Usuario usuario(Long id, String nombre, Rol rol, Area area) {
        return new Usuario(id, nombre, nombre.toLowerCase().replace(' ', '.') + "@demo.com", rol, area);
    }

    static UsuarioAutenticado autenticado(Usuario u) {
        return new UsuarioAutenticado(u.getId(), u.getNombre(), u.getEmail(), "x", u.getRol(), u.getArea().getId(),
                u.getArea().getNombre());
    }

    static Procedimiento procedimiento(Long id, String codigo, boolean obligatorio, EstadoProcedimiento estado, Area... areas) {
        Procedimiento p = new Procedimiento();
        p.setId(id);
        p.setCodigo(codigo);
        p.setNombre("Procedimiento " + codigo);
        p.setTipo(TipoProcedimiento.PROCEDIMIENTO);
        p.setBio("bio");
        p.setFechaCreacion(LocalDate.of(2020, 1, 1));
        p.setMinutosPorUso(10);
        p.setUsosPorSemana(4);
        p.setBeneficio("algo");
        p.setObligatorio(obligatorio);
        p.setBaseObligatoriedad(obligatorio ? "ISO 9001 7.1.3" : null);
        p.setEstado(estado);
        p.setAreas(new LinkedHashSet<>(List.of(areas)));
        return p;
    }

    static EvaluacionProcedimiento.Conteo conteo(Area a, int likes, int nopes) {
        return new EvaluacionProcedimiento.Conteo(a.getId(), a.getNombre(), likes, nopes);
    }
}
