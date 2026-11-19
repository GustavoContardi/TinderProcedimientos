package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.servicio.ObjecionService;
import ar.gob.defensoria.tinder.web.dto.EvaluarObjecionRequest;
import ar.gob.defensoria.tinder.web.dto.GrupoObjecionesDTO;
import ar.gob.defensoria.tinder.web.dto.ObjecionDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/objeciones")
@Tag(name = "Objeciones", description = "La voz anónima de la minoría")
public class ObjecionController {

    private final ObjecionService objeciones;

    public ObjecionController(ObjecionService objeciones) {
        this.objeciones = objeciones;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('JUEZ_PAZ','DIRECCION')")
    @Operation(summary = "Objeciones agrupadas por procedimiento y categoría (nunca hay datos de autor)")
    public List<GrupoObjecionesDTO> agrupadas(@RequestParam(required = false) Long procedimientoId) {
        return objeciones.agrupadas(procedimientoId);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    @Operation(summary = "Evaluar: fundada o no y respuesta. Crea una mejora (origen OBJECION).")
    public ObjecionDTO evaluar(@PathVariable UUID id, @Valid @RequestBody EvaluarObjecionRequest req,
                               @AuthenticationPrincipal UsuarioAutenticado juez) {
        return objeciones.evaluar(id, req, juez);
    }
}
