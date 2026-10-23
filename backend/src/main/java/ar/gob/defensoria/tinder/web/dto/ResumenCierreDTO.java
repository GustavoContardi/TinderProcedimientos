package ar.gob.defensoria.tinder.web.dto;

import java.util.List;

public record ResumenCierreDTO(CicloDTO ciclo, int resultadosCalculados, List<TransicionDTO> transiciones) {
}
