package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.TinderProperties;
import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ar.gob.defensoria.tinder.servicio.ProcedimientoService.blancoANull;

/**
 * Circuito de las objeciones de la minoría. El juez de paz las ve agrupadas y nunca ve datos de autor
 * (no existen). Toda objeción evaluada genera una mejora: nadie queda sin respuesta.
 */
@Service
public class ObjecionService {

    private final ObjecionRepository objeciones;
    private final ProcedimientoRepository procedimientos;
    private final CicloVotacionRepository ciclos;
    private final CasamientoRepository casamientos;
    private final MatrizParticipacionRepository matriz;
    private final SegundaCitaRepository citas;
    private final MejoraService mejoras;
    private final MaquinaEstados maquina;
    private final TinderProperties props;

    public ObjecionService(ObjecionRepository objeciones, ProcedimientoRepository procedimientos,
                           CicloVotacionRepository ciclos, CasamientoRepository casamientos,
                           MatrizParticipacionRepository matriz, SegundaCitaRepository citas, MejoraService mejoras,
                           MaquinaEstados maquina, TinderProperties props) {
        this.objeciones = objeciones;
        this.procedimientos = procedimientos;
        this.ciclos = ciclos;
        this.casamientos = casamientos;
        this.matriz = matriz;
        this.citas = citas;
        this.mejoras = mejoras;
        this.maquina = maquina;
        this.props = props;
    }

    /** Objeciones agrupadas por procedimiento y categoría (las que tienen pendientes, primero). */
    @Transactional(readOnly = true)
    public List<GrupoObjecionesDTO> agrupadas(Long procedimientoId) {
        List<Objecion> todas = procedimientoId != null ? objeciones.findByProcedimientoId(procedimientoId) : objeciones.findAll();
        Map<Long, String> nombresCiclo = ciclos.findAll().stream()
                .collect(Collectors.toMap(CicloVotacion::getId, CicloVotacion::getNombre));
        Map<Long, Procedimiento> procs = procedimientos.findAllById(
                        todas.stream().map(Objecion::getProcedimientoId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Procedimiento::getId, Function.identity()));

        Map<Long, List<Objecion>> porProc = todas.stream()
                .collect(Collectors.groupingBy(Objecion::getProcedimientoId, TreeMap::new, Collectors.toList()));
        List<GrupoObjecionesDTO> grupos = new ArrayList<>();
        porProc.forEach((pid, lista) -> {
            Procedimiento p = procs.get(pid);
            Map<CategoriaObjecion, List<Objecion>> porCat = lista.stream()
                    .collect(Collectors.groupingBy(Objecion::getCategoria, () -> new EnumMap<>(CategoriaObjecion.class),
                            Collectors.toList()));
            List<GrupoObjecionesDTO.Categoria> categorias = porCat.entrySet().stream()
                    .sorted(Comparator.comparing((Map.Entry<CategoriaObjecion, List<Objecion>> e) -> -e.getValue().size())
                            .thenComparing(Map.Entry::getKey))
                    .map(e -> new GrupoObjecionesDTO.Categoria(e.getKey(), e.getValue().size(), e.getValue().stream()
                            .sorted(Comparator.comparing(Objecion::getEstado).thenComparing(o -> o.getId().toString()))
                            .map(o -> dto(o, nombresCiclo)).toList()))
                    .toList();
            int pendientes = (int) lista.stream().filter(o -> o.getEstado() == EstadoObjecion.PENDIENTE).count();
            grupos.add(new GrupoObjecionesDTO(pid, p.getCodigo(), p.getNombre(), p.getEstado(), lista.size(), pendientes,
                    categorias));
        });
        grupos.sort(Comparator.comparing((GrupoObjecionesDTO g) -> -g.pendientes()).thenComparing(GrupoObjecionesDTO::codigo));
        return grupos;
    }

    /**
     * El juez marca fundada / no fundada y responde. No fundada exige respuesta; fundada sin respuesta queda EVALUADA
     * (y bloquea el casamiento). La primera evaluación crea una mejora (responsable: el dueño; fecha: 30 días o la indicada).
     * Una objeción de boda o aniversario fundada posterga el casamiento y manda el procedimiento a segunda cita.
     */
    @Transactional
    public ObjecionDTO evaluar(UUID id, EvaluarObjecionRequest req, UsuarioAutenticado juez) {
        Objecion o = objeciones.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe la objeción " + id));
        String respuesta = blancoANull(req.respuesta());
        if (!req.fundada() && respuesta == null) {
            throw ApiException.reglaNegocio("RESPUESTA_OBLIGATORIA",
                    "Nadie queda sin respuesta: si no es fundada, explicá por qué.");
        }
        Procedimiento p = procedimientos.findById(o.getProcedimientoId())
                .orElseThrow(() -> ApiException.noEncontrado("No existe el procedimiento " + o.getProcedimientoId()));
        boolean primeraVez = o.getEstado() == EstadoObjecion.PENDIENTE;
        boolean eraFundada = Boolean.TRUE.equals(o.getFundada());
        o.evaluar(req.fundada(), respuesta, juez.id());

        if (primeraVez) {
            String etiqueta = categoriaLegible(o.getCategoria());
            String descripcion = req.fundada()
                    ? "Atender objeción fundada (" + etiqueta + ")" + (o.getTexto() != null ? ": «" + o.getTexto() + "»" : ".")
                    : "Contarle al equipo por qué no se cambia (" + etiqueta + "): " + respuesta;
            mejoras.crearAutomatica(OrigenMejora.OBJECION, o.getId().toString(), p, descripcion, juez.id(),
                    req.fechaCompromiso());
        } else if (req.fechaCompromiso() != null) {
            mejoras.deOrigen(OrigenMejora.OBJECION, o.getId().toString())
                    .ifPresent(m -> m.setFechaCompromiso(req.fechaCompromiso()));
        }
        if (req.fundada() && !eraFundada && o.getOrigen() != OrigenObjecion.VOTO) {
            postergarCasamiento(p);
        }
        Map<Long, String> nombresCiclo = o.getCicloId() == null ? Map.of()
                : ciclos.findById(o.getCicloId()).map(c -> Map.of(c.getId(), c.getNombre())).orElse(Map.of());
        return dto(o, nombresCiclo);
    }

    private void postergarCasamiento(Procedimiento p) {
        casamientos.findFirstByProcedimientoIdAndEstadoOrderByIdDesc(p.getId(), EstadoCasamiento.VIGENTE)
                .ifPresent(c -> c.setEstado(EstadoCasamiento.POSTERGADO));
        if (p.getEstado() == EstadoProcedimiento.CASADO) {
            maquina.transicionar(p, EstadoProcedimiento.SEGUNDA_CITA);
            citas.save(new SegundaCita(p, null, LocalDate.now().plusDays(props.diasHastaSegundaCita())));
        }
    }

    /**
     * "¿Alguien se opone?": objeción anónima en la boda (o en el aniversario, si ya está cerca).
     * La puede hacer cualquiera que esté en la matriz del procedimiento. No se guarda quién.
     */
    @Transactional
    public ObjecionRecibidaDTO objetarCasamiento(Long casamientoId, ObjecionRequest req, UsuarioAutenticado usuario) {
        Casamiento c = casamientos.findById(casamientoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el casamiento " + casamientoId));
        if (c.getEstado() != EstadoCasamiento.VIGENTE) {
            throw ApiException.reglaNegocio("CASAMIENTO_NO_VIGENTE",
                    "Ese casamiento está " + c.getEstado().name().toLowerCase() + ": ya no se puede objetar.");
        }
        Procedimiento p = c.getProcedimiento();
        if (!matriz.existsById(new MatrizParticipacion.Id(usuario.id(), p.getId()))) {
            throw ApiException.prohibido("Solo se puede oponer quien participa de " + p.getCodigo() + ".");
        }
        boolean aniversario = !LocalDate.now().isBefore(c.getFechaAniversario().minusDays(props.diasAvisoAniversario()));
        OrigenObjecion origen = aniversario ? OrigenObjecion.ANIVERSARIO : OrigenObjecion.BODA;
        objeciones.save(new Objecion(p.getId(), null, usuario.areaId(), origen, req.categoria(), blancoANull(req.texto())));
        return new ObjecionRecibidaDTO(origen, "Objeción recibida, anónima. El juez de paz la va a evaluar: nadie queda sin respuesta.");
    }

    private ObjecionDTO dto(Objecion o, Map<Long, String> nombresCiclo) {
        Optional<Mejora> m = mejoras.deOrigen(OrigenMejora.OBJECION, o.getId().toString());
        return new ObjecionDTO(o.getId(), o.getProcedimientoId(),
                o.getCicloId() != null ? nombresCiclo.get(o.getCicloId()) : null, o.getOrigen(), o.getCategoria(),
                o.getTexto(), o.getFundada(), o.getRespuesta(), o.getEstado(),
                m.map(Mejora::getId).orElse(null), m.map(Mejora::getCodigo).orElse(null));
    }

    public static String categoriaLegible(CategoriaObjecion c) {
        return switch (c) {
            case DUPLICA_OTRO_REGISTRO -> "Duplica otro registro";
            case DEMASIADOS_PASOS -> "Demasiados pasos";
            case NO_APLICA_A_MI_TAREA -> "No aplica a mi tarea";
            case DESACTUALIZADO -> "Desactualizado";
            case FALTA_ALGO -> "Le falta algo";
            case NO_SE_PUEDE_EN_URGENCIAS -> "No se puede en urgencias";
            case OTRO -> "Otro";
        };
    }
}
