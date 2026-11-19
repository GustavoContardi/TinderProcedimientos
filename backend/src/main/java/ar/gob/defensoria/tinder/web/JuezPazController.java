package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.servicio.JuezPazService;
import ar.gob.defensoria.tinder.web.dto.CalendarioDTO;
import ar.gob.defensoria.tinder.web.dto.DesignacionDTO;
import ar.gob.defensoria.tinder.web.dto.DesignacionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/juez-de-paz")
@Tag(name = "Juez de paz", description = "Rotación anual y calendario del trimestre")
public class JuezPazController {

    private final JuezPazService juez;

    public JuezPazController(JuezPazService juez) {
        this.juez = juez;
    }

    @GetMapping("/designaciones")
    @PreAuthorize("hasAnyRole('JUEZ_PAZ','DIRECCION')")
    public List<DesignacionDTO> historial() {
        return juez.historial();
    }

    @PostMapping("/designaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DIRECCION')")
    @Operation(summary = "Designar juez de paz del área: cierra la vigente y el anterior vuelve a EMPLEADO")
    public DesignacionDTO designar(@Valid @RequestBody DesignacionRequest req) {
        return juez.designar(req);
    }

    @GetMapping("/calendario")
    @PreAuthorize("hasAnyRole('JUEZ_PAZ','DIRECCION')")
    @Operation(summary = "Checklist calculado del trimestre")
    public CalendarioDTO calendario() {
        return juez.calendario();
    }
}
