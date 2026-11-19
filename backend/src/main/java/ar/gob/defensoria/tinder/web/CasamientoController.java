package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.servicio.CasamientoService;
import ar.gob.defensoria.tinder.servicio.ObjecionService;
import ar.gob.defensoria.tinder.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/casamientos")
@Tag(name = "Casamientos", description = "Hasta que la burocracia nos separe")
public class CasamientoController {

    private final CasamientoService casamientos;
    private final ObjecionService objeciones;

    public CasamientoController(CasamientoService casamientos, ObjecionService objeciones) {
        this.casamientos = casamientos;
        this.objeciones = objeciones;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    @Operation(summary = "Registrar casamiento (oficia el juez de paz; grupal si son 2 o más áreas)")
    public CasamientoDTO casar(@Valid @RequestBody CasamientoRequest req) {
        return casamientos.casar(req);
    }

    @GetMapping
    public List<CasamientoDTO> listar() {
        return casamientos.listar();
    }

    @GetMapping("/aniversarios")
    @PreAuthorize("hasAnyRole('JUEZ_PAZ','DIRECCION')")
    @Operation(summary = "Informe de los casamientos con aniversario en los próximos 15 días (o vencido)")
    public List<AniversarioDTO> aniversarios() {
        return casamientos.aniversarios();
    }

    @GetMapping("/{id}")
    public CasamientoDTO obtener(@PathVariable Long id) {
        return casamientos.obtener(id);
    }

    @PostMapping("/{id}/objeciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "¿Alguien se opone? Objeción anónima de boda (o de aniversario si ya está cerca)")
    public ObjecionRecibidaDTO objetar(@PathVariable Long id, @Valid @RequestBody ObjecionRequest req,
                                       @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return objeciones.objetarCasamiento(id, req, usuario);
    }

    @PostMapping("/{id}/renovar")
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    public CasamientoDTO renovar(@PathVariable Long id, @Valid @RequestBody(required = false) AccionAniversarioRequest req) {
        return casamientos.renovar(id, req != null ? req.usoReal() : null);
    }

    @PostMapping("/{id}/segunda-cita")
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    public CasamientoDTO segundaCita(@PathVariable Long id, @Valid @RequestBody(required = false) AccionAniversarioRequest req,
                                     @AuthenticationPrincipal UsuarioAutenticado juez) {
        return casamientos.segundaCita(id, req != null ? req.usoReal() : null, juez);
    }

    @PostMapping("/{id}/divorciar")
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    public CasamientoDTO divorciar(@PathVariable Long id, @Valid @RequestBody(required = false) AccionAniversarioRequest req) {
        return casamientos.divorciar(id, req != null ? req.usoReal() : null);
    }
}
