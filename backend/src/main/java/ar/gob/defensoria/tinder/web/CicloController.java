package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.servicio.CicloService;
import ar.gob.defensoria.tinder.web.dto.CicloDTO;
import ar.gob.defensoria.tinder.web.dto.CicloRequest;
import ar.gob.defensoria.tinder.web.dto.ResumenCierreDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ciclos")
@Tag(name = "Ciclos")
public class CicloController {

    private final CicloService ciclos;

    public CicloController(CicloService ciclos) {
        this.ciclos = ciclos;
    }

    @GetMapping
    public List<CicloDTO> listar() {
        return ciclos.listar();
    }

    @GetMapping("/actual")
    @Operation(summary = "Ciclo abierto (204 si no hay ninguno)")
    public ResponseEntity<CicloDTO> actual() {
        return ciclos.actual().map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CALIDAD')")
    public CicloDTO abrir(@Valid @RequestBody CicloRequest req) {
        return ciclos.abrir(req);
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasRole('CALIDAD')")
    @Operation(summary = "Cierra el ciclo, calcula resultados por área y aplica las transiciones de estado")
    public ResumenCierreDTO cerrar(@PathVariable Long id) {
        return ciclos.cerrar(id);
    }
}
