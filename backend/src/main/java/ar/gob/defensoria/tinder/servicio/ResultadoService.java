package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.CicloVotacion;
import ar.gob.defensoria.tinder.dominio.EstadoCiclo;
import ar.gob.defensoria.tinder.dominio.ResultadoCiclo;
import ar.gob.defensoria.tinder.dominio.Veredicto;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.CicloVotacionRepository;
import ar.gob.defensoria.tinder.repositorio.ResultadoCicloRepository;
import ar.gob.defensoria.tinder.web.dto.ResultadoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class ResultadoService {

    private final ResultadoCicloRepository resultados;
    private final CicloVotacionRepository ciclos;

    public ResultadoService(ResultadoCicloRepository resultados, CicloVotacionRepository ciclos) {
        this.resultados = resultados;
        this.ciclos = ciclos;
    }

    /** EMPLEADO: siempre su área. CALIDAD/DIRECCION: todas (o la pedida). Por defecto, el último ciclo cerrado. */
    @Transactional(readOnly = true)
    public List<ResultadoDTO> listar(UsuarioAutenticado usuario, Long cicloId, Long areaId) {
        Optional<CicloVotacion> ciclo = cicloId != null
                ? Optional.of(ciclos.findById(cicloId).orElseThrow(() -> ApiException.noEncontrado("No existe el ciclo " + cicloId)))
                : ciclos.findFirstByEstadoOrderByFechaFinDesc(EstadoCiclo.CERRADO);
        if (ciclo.isEmpty()) {
            return List.of();
        }
        Long filtroArea = usuario.esEmpleado() ? usuario.areaId() : areaId;
        return resultados.findByCicloIdOrderByAreaNombreAscProcedimientoCodigoAsc(ciclo.get().getId()).stream()
                .filter(r -> filtroArea == null || r.getArea().getId().equals(filtroArea))
                .map(ResultadoService::aDto)
                .toList();
    }

    static ResultadoDTO aDto(ResultadoCiclo r) {
        var p = r.getProcedimiento();
        boolean ocultar = r.getVeredicto() == Veredicto.QUORUM_INSUFICIENTE;
        Integer pctLike = ocultar ? null : r.getPctLike().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
        String mensaje = switch (r.getVeredicto()) {
            case MATCH -> "¡Es un match! Preparen la libreta.";
            case RECHAZO -> "Le dejaron el visto. Se viene el velorio.";
            case SEGUNDA_CITA -> "Votos divididos: segunda cita el trimestre que viene.";
            case QUORUM_INSUFICIENTE -> "Pocos votos, por anonimato no mostramos números";
        };
        return new ResultadoDTO(r.getCiclo().getId(), r.getCiclo().getNombre(), p.getId(), p.getCodigo(), p.getNombre(),
                r.getArea().getId(), r.getArea().getNombre(), r.getVeredicto(),
                ocultar ? null : r.getLikes(), ocultar ? null : r.getNopes(), ocultar ? null : r.getTotal(),
                pctLike, ocultar ? null : 100 - pctLike, mensaje);
    }
}
