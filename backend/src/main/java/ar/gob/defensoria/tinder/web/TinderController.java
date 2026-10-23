package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.servicio.TinderService;
import ar.gob.defensoria.tinder.web.dto.PerfilDTO;
import ar.gob.defensoria.tinder.web.dto.VotoRequest;
import ar.gob.defensoria.tinder.web.dto.VotoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tinder")
@Tag(name = "Tinder", description = "Mazo y voto anónimo")
public class TinderController {

    private final TinderService tinder;

    public TinderController(TinderService tinder) {
        this.tinder = tinder;
    }

    @GetMapping("/mazo")
    @Operation(summary = "Procedimientos de mi área que todavía no voté en el ciclo abierto (orden aleatorio)")
    public List<PerfilDTO> mazo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return tinder.mazo(usuario);
    }

    @PostMapping("/votos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Votar de forma anónima (LIKE = me sirve, NOPE = me sobra). 409 si ya votó.")
    public VotoResponse votar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                              @Valid @RequestBody VotoRequest req) {
        return tinder.votar(usuario, req.procedimientoId(), req.valor());
    }
}
