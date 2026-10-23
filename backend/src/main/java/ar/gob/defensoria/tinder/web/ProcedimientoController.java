package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.servicio.ProcedimientoService;
import ar.gob.defensoria.tinder.web.dto.ProcedimientoDTO;
import ar.gob.defensoria.tinder.web.dto.ProcedimientoRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedimientos")
@Tag(name = "Procedimientos")
public class ProcedimientoController {

    private final ProcedimientoService procedimientos;

    public ProcedimientoController(ProcedimientoService procedimientos) {
        this.procedimientos = procedimientos;
    }

    @GetMapping
    public List<ProcedimientoDTO> listar(@RequestParam(required = false) EstadoProcedimiento estado) {
        return procedimientos.listar(estado);
    }

    @GetMapping("/{id}")
    public ProcedimientoDTO obtener(@PathVariable Long id) {
        return procedimientos.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CALIDAD')")
    public ProcedimientoDTO crear(@Valid @RequestBody ProcedimientoRequest req) {
        return procedimientos.crear(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CALIDAD')")
    public ProcedimientoDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProcedimientoRequest req) {
        return procedimientos.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CALIDAD')")
    public void eliminar(@PathVariable Long id) {
        procedimientos.eliminar(id);
    }
}
