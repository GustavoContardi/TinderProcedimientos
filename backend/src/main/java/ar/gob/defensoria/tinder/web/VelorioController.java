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
    @PreAuthorize("hasRole('CALIDAD')")
    @Operation(summary = "Causa de muerte y epitafio sugeridos para un procedimiento pendiente de velorio")
    public SugerenciaVelorioDTO sugerencia(@RequestParam Long procedimientoId) {
        return velorios.sugerencia(procedimientoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CALIDAD')")
    public VelorioDTO crear(@Valid @RequestBody VelorioRequest req) {
        return velorios.crear(req);
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
    @PreAuthorize("hasRole('DIRECCION')")
    @Operation(summary = "Firmar el acta de defunción: el procedimiento pasa a VELADO (nunca se borra)")
    public VelorioDTO firmar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return velorios.firmar(id, usuario.id());
    }
}
