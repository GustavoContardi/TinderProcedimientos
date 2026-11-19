package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.CategoriaObjecion;
import ar.gob.defensoria.tinder.dominio.EstadoCasamiento;
import ar.gob.defensoria.tinder.dominio.OrigenObjecion;

import java.time.LocalDate;
import java.util.List;

/** La libreta completa. */
public record CasamientoDTO(
        Long id,
        String numero,
        Long procedimientoId,
        String codigo,
        String nombre,
        boolean obligatorio,
        int versionProcedimiento,
        String versionExpress,
        LocalDate fecha,
        String votos,
        String padrinoNombre,
        String duenoNombre,
        String oficianteNombre,
        boolean grupal,
        String condiciones,
        String informeAniversario,
        LocalDate fechaAniversario,
        long diasParaAniversario,
        EstadoCasamiento estado,
        List<String> areas,
        List<Testigo> testigos,
        List<PctArea> resultados,
        List<ObjecionTratada> objecionesTratadas) {

    public record Testigo(Long id, String nombre, String areaNombre) {
    }

    public record PctArea(String areaNombre, int pct) {
    }

    public record ObjecionTratada(CategoriaObjecion categoria, OrigenObjecion origen, Boolean fundada, String respuesta) {
    }
}
