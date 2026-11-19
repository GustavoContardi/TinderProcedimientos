package ar.gob.defensoria.tinder.web.dto;

import jakarta.validation.constraints.Size;

/** Campo libre de "uso real" que el juez completa en el aniversario. */
public record AccionAniversarioRequest(@Size(max = 2000) String usoReal) {
}
