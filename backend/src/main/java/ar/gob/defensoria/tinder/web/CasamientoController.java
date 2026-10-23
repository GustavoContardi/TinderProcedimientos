package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.servicio.CasamientoService;
import ar.gob.defensoria.tinder.web.dto.CasamientoDTO;
import ar.gob.defensoria.tinder.web.dto.CasamientoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/casamientos")
@Tag(name = "Casamientos", description = "Hasta que la burocracia nos separe")
public class CasamientoController {

    private final CasamientoService casamientos;

    public CasamientoController(CasamientoService casamientos) {
        this.casamientos = casamientos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CALIDAD')")
    @Operation(summary = "Registrar casamiento (grupal si son 2 o más áreas). Oficia alguien de la Dirección.")
    public CasamientoDTO casar(@Valid @RequestBody CasamientoRequest req) {
        return casamientos.casar(req);
    }

    @GetMapping
    public List<CasamientoDTO> listar() {
        return casamientos.listar();
    }

    @GetMapping("/aniversarios")
    @PreAuthorize("hasAnyRole('CALIDAD','DIRECCION')")
    @Operation(summary = "Casamientos vigentes con aniversario en los próximos 15 días (o vencido)")
    public List<CasamientoDTO> aniversarios() {
        return casamientos.aniversarios();
    }

    @GetMapping("/{id}")
    public CasamientoDTO obtener(@PathVariable Long id) {
        return casamientos.obtener(id);
    }

    @PostMapping("/{id}/renovar")
    @PreAuthorize("hasAnyRole('CALIDAD','DIRECCION')")
    public CasamientoDTO renovar(@PathVariable Long id) {
        return casamientos.renovar(id);
    }

    @PostMapping("/{id}/divorciar")
    @PreAuthorize("hasAnyRole('CALIDAD','DIRECCION')")
    public CasamientoDTO divorciar(@PathVariable Long id) {
        return casamientos.divorciar(id);
    }
}
