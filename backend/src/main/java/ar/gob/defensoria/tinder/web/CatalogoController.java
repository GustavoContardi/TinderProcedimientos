package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.dominio.Rol;
import ar.gob.defensoria.tinder.repositorio.AreaRepository;
import ar.gob.defensoria.tinder.repositorio.UsuarioRepository;
import ar.gob.defensoria.tinder.web.dto.AreaDTO;
import ar.gob.defensoria.tinder.web.dto.UsuarioDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Catálogos", description = "Áreas y usuarios para los formularios")
public class CatalogoController {

    private final AreaRepository areas;
    private final UsuarioRepository usuarios;

    public CatalogoController(AreaRepository areas, UsuarioRepository usuarios) {
        this.areas = areas;
        this.usuarios = usuarios;
    }

    @GetMapping("/areas")
    @Transactional(readOnly = true)
    public List<AreaDTO> areas() {
        return areas.findAll(Sort.by("nombre")).stream().map(AreaDTO::de).toList();
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAnyRole('CALIDAD','DIRECCION')")
    @Transactional(readOnly = true)
    public List<UsuarioDTO> usuarios(@RequestParam(required = false) Rol rol) {
        var lista = rol == null ? usuarios.findAllByOrderByNombre() : usuarios.findByRolOrderByNombre(rol);
        return lista.stream().map(UsuarioDTO::de).toList();
    }
}
