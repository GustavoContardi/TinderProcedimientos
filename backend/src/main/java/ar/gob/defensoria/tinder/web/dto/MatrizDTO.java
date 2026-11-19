package ar.gob.defensoria.tinder.web.dto;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;

import java.util.List;

/** Grilla usuario × procedimiento. */
public record MatrizDTO(List<UsuarioDTO> usuarios, List<Proc> procedimientos, List<CeldaDTO> celdas) {

    public record Proc(Long id, String codigo, String nombre, EstadoProcedimiento estado, List<String> areas) {
    }
}
