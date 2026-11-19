package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.EstadoSegundaCita;
import ar.gob.defensoria.tinder.servicio.SegundaCitaService;
import ar.gob.defensoria.tinder.web.dto.RealizarCitaRequest;
import ar.gob.defensoria.tinder.web.dto.SegundaCitaDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/segundas-citas")
@Tag(name = "Segundas citas", description = "30 minutos, minuta, decisión y votación rápida")
public class SegundaCitaController {

    private final SegundaCitaService citas;

    public SegundaCitaController(SegundaCitaService citas) {
        this.citas = citas;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('JUEZ_PAZ','DIRECCION')")
    @Operation(summary = "Segundas citas (por defecto, las CONVOCADAS)")
    public List<SegundaCitaDTO> listar(@RequestParam(required = false, defaultValue = "CONVOCADA") EstadoSegundaCita estado,
                                       @RequestParam(required = false, defaultValue = "false") boolean todas) {
        return citas.listar(todas ? null : estado);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('JUEZ_PAZ','DIRECCION')")
    public SegundaCitaDTO obtener(@PathVariable Long id) {
        return citas.obtener(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('JUEZ_PAZ')")
    @Operation(summary = "Minuta + decisión: la cita queda REALIZADA y, si corresponde, se abre la votación rápida de 7 días")
    public SegundaCitaDTO realizar(@PathVariable Long id, @Valid @RequestBody RealizarCitaRequest req,
                                   @AuthenticationPrincipal UsuarioAutenticado juez) {
        return citas.realizar(id, req, juez);
    }
}
