package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.AlertaRepository;
import ar.gob.defensoria.tinder.web.dto.AlertaDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertaService {

    private final AlertaRepository alertas;

    public AlertaService(AlertaRepository alertas) {
        this.alertas = alertas;
    }

    @Transactional(readOnly = true)
    public List<AlertaDTO> listar() {
        return alertas.findAllByOrderByRevisadaAscCreatedAtDesc().stream().map(AlertaDTO::de).toList();
    }

    @Transactional
    public AlertaDTO revisar(Long id) {
        var a = alertas.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe la alerta " + id));
        a.marcarRevisada();
        return AlertaDTO.de(a);
    }
}
