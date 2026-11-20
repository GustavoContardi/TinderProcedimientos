package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.TinderProperties;
import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.MejoraRepository;
import ar.gob.defensoria.tinder.repositorio.ProcedimientoRepository;
import ar.gob.defensoria.tinder.repositorio.UsuarioRepository;
import ar.gob.defensoria.tinder.web.dto.MejoraDTO;
import ar.gob.defensoria.tinder.web.dto.MejoraRequest;
import ar.gob.defensoria.tinder.web.dto.MejoraUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ar.gob.defensoria.tinder.servicio.ProcedimientoService.blancoANull;

/** Tablero de mejoras: lo ven todos, lo editan el juez de paz y el responsable de cada mejora. */
@Service
public class MejoraService {

    public static final Set<EstadoMejora> CERRADAS = EnumSet.of(EstadoMejora.RESUELTA, EstadoMejora.DESCARTADA);

    private final MejoraRepository mejoras;
    private final ProcedimientoRepository procedimientos;
    private final UsuarioRepository usuarios;
    private final TinderProperties props;

    public MejoraService(MejoraRepository mejoras, ProcedimientoRepository procedimientos, UsuarioRepository usuarios,
                         TinderProperties props) {
        this.mejoras = mejoras;
        this.procedimientos = procedimientos;
        this.usuarios = usuarios;
        this.props = props;
    }

    /** Mejora automática con responsable = dueño (o el padrino, o quien la dispara) y fecha a N días. */
    @Transactional
    public Mejora crearAutomatica(OrigenMejora origen, String origenId, Procedimiento p, String descripcion,
                                  Long actorId, LocalDate fechaCompromiso) {
        Usuario responsable = p.getDueno() != null ? p.getDueno()
                : p.getAutor() != null ? p.getAutor()
                : usuarios.findById(actorId).orElseThrow(() -> ApiException.noEncontrado("No existe el usuario " + actorId));
        LocalDate fecha = fechaCompromiso != null ? fechaCompromiso : LocalDate.now().plusDays(props.diasCompromisoMejora());
        return mejoras.save(new Mejora(siguienteCodigo(), origen, origenId, p, descripcion, responsable, fecha));
    }

    public Optional<Mejora> deOrigen(OrigenMejora origen, String origenId) {
        return mejoras.findFirstByOrigenAndOrigenId(origen, origenId);
    }

    private String siguienteCodigo() {
        return "M-%03d".formatted(mejoras.siguienteNumero());
    }

    @Transactional(readOnly = true)
    public List<MejoraDTO> listar(Long procedimientoId, OrigenMejora origen, EstadoMejora estado) {
        return mejoras.findAllByOrderByFechaCompromisoAscIdAsc().stream()
                .filter(m -> procedimientoId == null || m.getProcedimiento().getId().equals(procedimientoId))
                .filter(m -> origen == null || m.getOrigen() == origen)
                .filter(m -> estado == null || m.getEstado() == estado)
                .map(Mapeador::mejora).toList();
    }

    @Transactional(readOnly = true)
    public List<MejoraDTO> abiertasDe(Long procedimientoId) {
        return mejoras.findByProcedimientoIdAndEstadoNotIn(procedimientoId, CERRADAS).stream()
                .map(Mapeador::mejora).toList();
    }

    @Transactional(readOnly = true)
    public MejoraDTO obtener(Long id) {
        return Mapeador.mejora(buscar(id));
    }

    /** Alta manual (juez de paz). */
    @Transactional
    public MejoraDTO crear(MejoraRequest req, UsuarioAutenticado actor) {
        Procedimiento p = procedimientos.findById(req.procedimientoId())
                .orElseThrow(() -> ApiException.noEncontrado("No existe el procedimiento " + req.procedimientoId()));
        Mejora m = crearAutomatica(req.origen(), blancoANull(req.origenId()), p, req.descripcion().trim(), actor.id(),
                req.fechaCompromiso());
        if (req.responsableId() != null) {
            m.setResponsable(usuario(req.responsableId()));
        }
        return Mapeador.mejora(m);
    }

    /** Editan el juez de paz y el responsable. RESUELTA o DESCARTADA exigen respuesta. */
    @Transactional
    public MejoraDTO actualizar(Long id, MejoraUpdateRequest req, UsuarioAutenticado actor) {
        Mejora m = buscar(id);
        boolean juez = actor.rol() == Rol.JUEZ_PAZ;
        boolean responsable = m.getResponsable().getId().equals(actor.id());
        if (!juez && !responsable) {
            throw ApiException.prohibido("Solo el juez de paz o el responsable (" + m.getResponsable().getNombre()
                    + ") pueden editar " + m.getCodigo() + ".");
        }
        if (!juez && !req.responsableId().equals(m.getResponsable().getId())) {
            throw ApiException.prohibido("Solo el juez de paz reasigna responsables.");
        }
        String respuesta = blancoANull(req.respuesta());
        if (CERRADAS.contains(req.estado()) && respuesta == null) {
            throw ApiException.reglaNegocio("RESPUESTA_OBLIGATORIA", "Para pasar " + m.getCodigo() + " a "
                    + (req.estado() == EstadoMejora.RESUELTA ? "resuelta" : "descartada")
                    + " registrá qué se hizo o por qué no se hace: toda mejora se cierra con una respuesta.");
        }
        m.setDescripcion(req.descripcion().trim());
        m.setFechaCompromiso(req.fechaCompromiso());
        if (!req.responsableId().equals(m.getResponsable().getId())) {
            m.setResponsable(usuario(req.responsableId()));
        }
        m.setEstado(req.estado());
        m.setRespuesta(respuesta);
        return Mapeador.mejora(m);
    }

    /** Solo se borra una mejora NUEVA cargada por error; el resto se descarta con respuesta. */
    @Transactional
    public void eliminar(Long id) {
        Mejora m = buscar(id);
        if (m.getEstado() != EstadoMejora.NUEVA) {
            throw ApiException.reglaNegocio("NO_SE_BORRA", m.getCodigo()
                    + " ya está en marcha: no se borra, se pasa a descartada con su respuesta.");
        }
        mejoras.delete(m);
    }

    private Usuario usuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el usuario " + id));
    }

    private Mejora buscar(Long id) {
        return mejoras.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe la mejora " + id));
    }
}
