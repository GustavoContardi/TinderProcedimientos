package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.EstadoMejora;
import ar.gob.defensoria.tinder.dominio.OrigenMejora;
import ar.gob.defensoria.tinder.servicio.MejoraService;
import ar.gob.defensoria.tinder.web.dto.MejoraDTO;
import ar.gob.defensoria.tinder.web.dto.MejoraRequest;
import ar.gob.defensoria.tinder.web.dto.MejoraUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mejoras")
@Tag(name = "Tablero de mejoras", description = "Toda objeción, acuerdo y acta genera acciones con responsable y fecha")
public class MejoraController {

    private final MejoraService mejoras;

    public MejoraController(MejoraService mejoras) {
        this.mejoras = mejoras;
    }

    @GetMapping
    public List<MejoraDTO> listar(@RequestParam(required = false) Long procedimientoId,
                                  @RequestParam(required = false) OrigenMejora origen,
                                  @RequestParam(required = false) EstadoMejora estado) {
        return mejoras.listar(procedimientoId, origen, estado);
    }

    @GetMapping("/{id}")
    public MejoraDTO obtener(@PathVariable Long id) {
        return mejoras.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    public MejoraDTO crear(@Valid @RequestBody MejoraRequest req, @AuthenticationPrincipal UsuarioAutenticado juez) {
        return mejoras.crear(req, juez);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editan el juez de paz y el responsable. RESUELTA o DESCARTADA exigen respuesta (422).")
    public MejoraDTO actualizar(@PathVariable Long id, @Valid @RequestBody MejoraUpdateRequest req,
                                @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return mejoras.actualizar(id, req, usuario);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    @Operation(summary = "Solo se borra una mejora NUEVA (cargada por error)")
    public void eliminar(@PathVariable Long id) {
        mejoras.eliminar(id);
    }
}
