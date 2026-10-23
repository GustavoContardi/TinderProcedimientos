package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.servicio.AlertaService;
import ar.gob.defensoria.tinder.web.dto.AlertaDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
@Tag(name = "Alertas", description = "Relaciones a distancia y otros dramas para que Calidad revise")
public class AlertaController {

    private final AlertaService alertas;

    public AlertaController(AlertaService alertas) {
        this.alertas = alertas;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CALIDAD','DIRECCION')")
    public List<AlertaDTO> listar() {
        return alertas.listar();
    }

    @PostMapping("/{id}/revisar")
    @PreAuthorize("hasRole('CALIDAD')")
    public AlertaDTO revisar(@PathVariable Long id) {
        return alertas.revisar(id);
    }
}
