package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.servicio.ProcedimientoService;
import ar.gob.defensoria.tinder.web.dto.ArchivoDTO;
import ar.gob.defensoria.tinder.web.dto.ContenidoRequest;
import ar.gob.defensoria.tinder.web.dto.ProcedimientoDTO;
import ar.gob.defensoria.tinder.web.dto.ProcedimientoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @Operation(summary = "Sin estado: todos menos los VELADOS (que solo están en /archivo)")
    public List<ProcedimientoDTO> listar(@RequestParam(required = false) EstadoProcedimiento estado) {
        return procedimientos.listar(estado);
    }

    @GetMapping("/archivo")
    @Operation(summary = "Archivo de obsoletos: procedimientos velados con su acta")
    public List<ArchivoDTO> archivo() {
        return procedimientos.archivo();
    }

    @GetMapping("/mios")
    @Operation(summary = "Procedimientos de los que soy dueño")
    public List<ProcedimientoDTO> mios(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return procedimientos.mios(usuario);
    }

    @GetMapping("/{id}")
    public ProcedimientoDTO obtener(@PathVariable Long id) {
        return procedimientos.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    public ProcedimientoDTO crear(@Valid @RequestBody ProcedimientoRequest req) {
        return procedimientos.crear(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    public ProcedimientoDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProcedimientoRequest req) {
        return procedimientos.actualizar(id, req);
    }

    @PutMapping("/{id}/contenido")
    @Operation(summary = "El dueño edita el contenido tras una segunda cita AJUSTAR_Y_REVOTAR (versión + 1)")
    public ProcedimientoDTO editarContenido(@PathVariable Long id, @Valid @RequestBody ContenidoRequest req,
                                            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return procedimientos.editarContenido(id, req, usuario);
    }

    @PostMapping("/{id}/publicar")
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    @Operation(summary = "BORRADOR → EN_TINDER")
    public ProcedimientoDTO publicar(@PathVariable Long id) {
        return procedimientos.publicar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    public void eliminar(@PathVariable Long id) {
        procedimientos.eliminar(id);
    }
}
