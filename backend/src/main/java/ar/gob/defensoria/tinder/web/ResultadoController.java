package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.servicio.ResultadoService;
import ar.gob.defensoria.tinder.web.dto.ResultadoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resultados")
@Tag(name = "Resultados")
public class ResultadoController {

    private final ResultadoService resultados;

    public ResultadoController(ResultadoService resultados) {
        this.resultados = resultados;
    }

    @GetMapping
    @Operation(summary = "Resultados agregados por área. EMPLEADO solo ve su área. Sin cicloId: último ciclo cerrado.")
    public List<ResultadoDTO> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @RequestParam(required = false) Long cicloId,
                                     @RequestParam(required = false) Long areaId) {
        return resultados.listar(usuario, cicloId, areaId);
    }
}
