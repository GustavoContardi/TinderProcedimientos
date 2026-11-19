package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.servicio.MatrizService;
import ar.gob.defensoria.tinder.web.dto.MatrizDTO;
import ar.gob.defensoria.tinder.web.dto.MatrizLoteDTO;
import ar.gob.defensoria.tinder.web.dto.MatrizLoteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matriz")
@Tag(name = "Matriz de participación", description = "Quién usa o ejecuta cada procedimiento")
public class MatrizController {

    private final MatrizService matriz;

    public MatrizController(MatrizService matriz) {
        this.matriz = matriz;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('JUEZ_PAZ','DIRECCION')")
    public MatrizDTO grilla(@RequestParam(required = false) Long areaId) {
        return matriz.grilla(areaId);
    }

    @PutMapping
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    @Operation(summary = "Guardado por lote (altas y bajas)")
    public MatrizLoteDTO guardar(@Valid @RequestBody MatrizLoteRequest req, @AuthenticationPrincipal UsuarioAutenticado juez) {
        return matriz.guardar(req, juez);
    }
}
