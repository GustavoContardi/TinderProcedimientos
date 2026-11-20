package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.CicloVotacionRepository;
import ar.gob.defensoria.tinder.repositorio.ResultadoCicloRepository;
import ar.gob.defensoria.tinder.repositorio.ResultadoProcedimientoRepository;
import ar.gob.defensoria.tinder.web.dto.ResultadoDTO;
import ar.gob.defensoria.tinder.web.dto.ResultadoProcedimientoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResultadoService {

    private final ResultadoCicloRepository resultados;
    private final ResultadoProcedimientoRepository resultadosProc;
    private final CicloVotacionRepository ciclos;

    public ResultadoService(ResultadoCicloRepository resultados, ResultadoProcedimientoRepository resultadosProc,
                            CicloVotacionRepository ciclos) {
        this.resultados = resultados;
        this.resultadosProc = resultadosProc;
        this.ciclos = ciclos;
    }

    /**
     * Resultados por procedimiento con sus áreas. EMPLEADO: solo las barras de su área. Resto: todas (o la pedida).
     * Por defecto, el último ciclo trimestral cerrado.
     */
    @Transactional(readOnly = true)
    public List<ResultadoProcedimientoDTO> listar(UsuarioAutenticado usuario, Long cicloId, Long areaId) {
        Optional<CicloVotacion> ciclo = cicloId != null
                ? Optional.of(ciclos.findById(cicloId).orElseThrow(() -> ApiException.noEncontrado("No existe el ciclo " + cicloId)))
                : ciclos.findFirstByEstadoAndTipoOrderByFechaFinDesc(EstadoCiclo.CERRADO, TipoCiclo.TRIMESTRAL);
        if (ciclo.isEmpty()) {
            return List.of();
        }
        CicloVotacion c = ciclo.get();
        Long filtroArea = usuario.esEmpleado() ? usuario.areaId() : areaId;
        Map<Long, ResultadoProcedimiento> porProc = resultadosProc.findByCicloId(c.getId()).stream()
                .collect(Collectors.toMap(r -> r.getProcedimiento().getId(), Function.identity()));
        Map<Long, List<ResultadoCiclo>> areasPorProc = resultados.findByCicloIdOrderByAreaNombreAscProcedimientoCodigoAsc(c.getId())
                .stream().collect(Collectors.groupingBy(r -> r.getProcedimiento().getId(), LinkedHashMap::new, Collectors.toList()));

        List<ResultadoProcedimientoDTO> lista = new ArrayList<>();
        areasPorProc.forEach((pid, filas) -> {
            List<ResultadoDTO> areas = filas.stream()
                    .filter(r -> filtroArea == null || r.getArea().getId().equals(filtroArea))
                    .map(ResultadoService::aDto).toList();
            if (filtroArea != null && areas.isEmpty()) {
                return;
            }
            Procedimiento p = filas.get(0).getProcedimiento();
            ResultadoProcedimiento rp = porProc.get(pid);
            lista.add(rp == null
                    ? new ResultadoProcedimientoDTO(c.getId(), c.getNombre(), c.getTipo(), pid, p.getCodigo(), p.getNombre(),
                    null, null, null, null, null, false, false,
                    "Ciclo anterior a la v2 (umbrales 60/60): solo hay resultados por área.", areas)
                    : dto(c, p, rp, areas));
        });
        lista.sort(Comparator.comparing(ResultadoProcedimientoDTO::codigo));
        return lista;
    }

    private static ResultadoProcedimientoDTO dto(CicloVotacion c, Procedimiento p, ResultadoProcedimiento r,
                                                 List<ResultadoDTO> areas) {
        boolean ocultar = r.getDestino() == Veredicto.QUORUM_INSUFICIENTE;
        Integer participacion = r.getHabilitados() == 0 ? 0 : (int) Math.round(100.0 * r.getVotantes() / r.getHabilitados());
        return new ResultadoProcedimientoDTO(c.getId(), c.getNombre(), c.getTipo(), p.getId(), p.getCodigo(), p.getNombre(),
                r.getDestino(), ocultar ? null : Mapeador.pct(r.getPctGlobal()), r.getVotantes(), r.getHabilitados(),
                participacion, r.isGrupal(), r.isRelacionADistancia(), r.getMensaje(), areas);
    }

    static ResultadoDTO aDto(ResultadoCiclo r) {
        var p = r.getProcedimiento();
        boolean ocultar = r.getVeredicto() == Veredicto.QUORUM_INSUFICIENTE;
        Integer pctLike = ocultar ? null : Mapeador.pct(r.getPctLike());
        String mensaje = switch (r.getVeredicto()) {
            case CASAMIENTO -> "70 % o más: esta área lo aprueba.";
            case VELORIO -> "Menos del 40 %: esta área lo rechaza.";
            case SEGUNDA_CITA -> "Entre 40 % y 70 %: resultado dividido.";
            case QUORUM_INSUFICIENTE -> "Votos insuficientes: por anonimato no se muestran números.";
            case PARTICIPACION_INSUFICIENTE -> "Participación insuficiente de la matriz.";
        };
        return new ResultadoDTO(r.getCiclo().getId(), r.getCiclo().getNombre(), p.getId(), p.getCodigo(), p.getNombre(),
                r.getArea().getId(), r.getArea().getNombre(), r.getVeredicto(),
                ocultar ? null : r.getLikes(), ocultar ? null : r.getNopes(), ocultar ? null : r.getTotal(),
                pctLike, ocultar ? null : 100 - pctLike, mensaje);
    }
}
