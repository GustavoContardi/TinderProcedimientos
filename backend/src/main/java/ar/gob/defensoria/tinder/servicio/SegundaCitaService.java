package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.TinderProperties;
import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.CicloVotacionRepository;
import ar.gob.defensoria.tinder.repositorio.ResultadoProcedimientoRepository;
import ar.gob.defensoria.tinder.repositorio.SegundaCitaRepository;
import ar.gob.defensoria.tinder.web.dto.CicloDTO;
import ar.gob.defensoria.tinder.web.dto.RealizarCitaRequest;
import ar.gob.defensoria.tinder.web.dto.SegundaCitaDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static ar.gob.defensoria.tinder.servicio.ProcedimientoService.blancoANull;

/**
 * Segunda cita: reunión de 30 minutos con minuta y decisión. Ninguna decisión cambia el destino por sí sola:
 * CASAR_CON_CONDICIONES y VELORIO abren una votación rápida de 7 días; AJUSTAR_Y_REVOTAR habilita al dueño
 * a editar y el juez elige entre esperar al próximo Tinder o lanzar la votación rápida.
 */
@Service
public class SegundaCitaService {

    private static final DateTimeFormatter AAMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    private final SegundaCitaRepository citas;
    private final CicloVotacionRepository ciclos;
    private final ResultadoProcedimientoRepository resultadosProc;
    private final MejoraService mejoras;
    private final TinderProperties props;

    public SegundaCitaService(SegundaCitaRepository citas, CicloVotacionRepository ciclos,
                              ResultadoProcedimientoRepository resultadosProc, MejoraService mejoras,
                              TinderProperties props) {
        this.citas = citas;
        this.ciclos = ciclos;
        this.resultadosProc = resultadosProc;
        this.mejoras = mejoras;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public List<SegundaCitaDTO> listar(EstadoSegundaCita estado) {
        var lista = estado == null ? citas.findAllByOrderByEstadoAscFechaProgramadaAscIdAsc()
                : citas.findByEstadoOrderByFechaProgramadaAscIdAsc(estado);
        return lista.stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public SegundaCitaDTO obtener(Long id) {
        return dto(buscar(id));
    }

    @Transactional
    public SegundaCitaDTO realizar(Long id, RealizarCitaRequest req, UsuarioAutenticado juez) {
        SegundaCita cita = buscar(id);
        if (cita.getEstado() == EstadoSegundaCita.REALIZADA) {
            throw ApiException.conflicto("CITA_REALIZADA", "Esa segunda cita ya se hizo. No hay tercera (por ahora).");
        }
        Procedimiento p = cita.getProcedimiento();
        if (p.getEstado() != EstadoProcedimiento.SEGUNDA_CITA) {
            throw ApiException.reglaNegocio("TRANSICION_INVALIDA", p.getCodigo() + " ya no está en segunda cita ("
                    + MaquinaEstados.legible(p.getEstado()) + ").");
        }
        DecisionSegundaCita decision = req.decision();
        if (decision == DecisionSegundaCita.VELORIO && p.isObligatorio()) {
            throw ApiException.reglaNegocio("OBLIGATORIO_NO_VELABLE",
                    p.getCodigo() + ": " + ClasificadorVeredicto.MENSAJE_OBLIGATORIO + ".");
        }
        String condiciones = blancoANull(req.condiciones());
        if (decision == DecisionSegundaCita.CASAR_CON_CONDICIONES && condiciones == null) {
            throw ApiException.reglaNegocio("FALTAN_CONDICIONES", "Casar con condiciones… ¿cuáles? Escribilas.");
        }
        cita.realizar(req.minuta().trim(), decision, condiciones);

        boolean lanzar = decision != DecisionSegundaCita.AJUSTAR_Y_REVOTAR || req.lanzarRapida();
        if (lanzar) {
            if (ciclos.existsByProcedimientoIdAndEstadoAndTipo(p.getId(), EstadoCiclo.ABIERTO, TipoCiclo.RAPIDA)) {
                throw ApiException.conflicto("RAPIDA_ABIERTA", p.getCodigo() + " ya tiene una votación rápida abierta.");
            }
            LocalDate hoy = LocalDate.now();
            cita.setCicloRapido(ciclos.save(CicloVotacion.rapida(nombreRapida(p, hoy), p, hoy,
                    hoy.plusDays(props.diasVotacionRapida()))));
        }
        String descripcion = switch (decision) {
            case AJUSTAR_Y_REVOTAR -> "Ajustar " + p.getCodigo() + " según la minuta de la segunda cita"
                    + (lanzar ? " antes de que cierre la votación rápida." : " para el próximo Tinder.");
            case CASAR_CON_CONDICIONES -> "Cumplir las condiciones del casamiento de " + p.getCodigo() + ": " + condiciones;
            case VELORIO -> "Preparar la posible baja de " + p.getCodigo() + " (herencia y reemplazo) mientras se vota.";
        };
        mejoras.crearAutomatica(OrigenMejora.SEGUNDA_CITA, String.valueOf(cita.getId()), p, descripcion, juez.id(), null);
        return dto(cita);
    }

    private String nombreRapida(Procedimiento p, LocalDate hoy) {
        String base = "Rápida " + p.getCodigo() + " " + hoy.format(AAMMDD);
        String nombre = base;
        for (int i = 2; ciclos.existsByNombreIgnoreCase(nombre); i++) {
            nombre = base + "-" + i;
        }
        return nombre;
    }

    private SegundaCitaDTO dto(SegundaCita c) {
        Procedimiento p = c.getProcedimiento();
        Optional<ResultadoProcedimiento> r = c.getCiclo() == null ? Optional.empty()
                : resultadosProc.findByCicloId(c.getCiclo().getId()).stream()
                .filter(x -> x.getProcedimiento().getId().equals(p.getId())).findFirst();
        boolean conNumeros = r.isPresent() && r.get().getDestino() != Veredicto.QUORUM_INSUFICIENTE;
        String motivo = r.map(ResultadoProcedimiento::getMensaje)
                .orElse(c.getCiclo() == null ? "Objeción fundada en la boda o en el aniversario." : null);
        return new SegundaCitaDTO(c.getId(), Mapeador.procedimiento(p),
                c.getCiclo() != null ? c.getCiclo().getId() : null, c.getCiclo() != null ? c.getCiclo().getNombre() : null,
                c.getFechaProgramada(), c.getEstado(), c.getMinuta(), c.getDecision(), c.getCondiciones(),
                c.getCicloRapido() != null ? CicloDTO.de(c.getCicloRapido()) : null,
                conNumeros ? Mapeador.pct(r.get().getPctGlobal()) : null,
                r.map(ResultadoProcedimiento::getVotantes).orElse(null),
                r.map(ResultadoProcedimiento::getHabilitados).orElse(null), motivo);
    }

    private SegundaCita buscar(Long id) {
        return citas.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe la segunda cita " + id));
    }
}
