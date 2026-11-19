package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.servicio.VelorioService;
import ar.gob.defensoria.tinder.web.dto.SugerenciaVelorioDTO;
import ar.gob.defensoria.tinder.web.dto.VelorioDTO;
import ar.gob.defensoria.tinder.web.dto.VelorioRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/velorios")
@Tag(name = "Velorios", description = "Con profundo alivio despedimos...")
public class VelorioController {

    private final VelorioService velorios;

    public VelorioController(VelorioService velorios) {
        this.velorios = velorios;
    }

    @GetMapping("/sugerencia")
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    @Operation(summary = "Causa y epitafio sugeridos para un procedimiento pendiente de velorio")
    public SugerenciaVelorioDTO sugerencia(@RequestParam Long procedimientoId) {
        return velorios.sugerencia(procedimientoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    @Operation(summary = "Labrar el acta de defunción (con reencarnación o motivo de no reemplazo)")
    public VelorioDTO crear(@Valid @RequestBody VelorioRequest req, @AuthenticationPrincipal UsuarioAutenticado juez) {
        return velorios.crear(req, juez);
    }

    @GetMapping
    public List<VelorioDTO> listar() {
        return velorios.listar();
    }

    @GetMapping("/{id}")
    public VelorioDTO obtener(@PathVariable Long id) {
        return velorios.obtener(id);
    }

    @PostMapping("/{id}/firmar")
    @Operation(summary = "Firma del juez de paz, la Dirección o el dueño. Con las 3, el procedimiento queda VELADO. 409 si repite.")
    public VelorioDTO firmar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return velorios.firmar(id, usuario);
    }
}
