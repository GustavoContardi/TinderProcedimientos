package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.TinderProperties;
import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.AniversarioDTO;
import ar.gob.defensoria.tinder.web.dto.CasamientoDTO;
import ar.gob.defensoria.tinder.web.dto.CasamientoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

import static ar.gob.defensoria.tinder.servicio.ProcedimientoService.blancoANull;

@Service
public class CasamientoService {

    private final CasamientoRepository casamientos;
    private final ProcedimientoRepository procedimientos;
    private final UsuarioRepository usuarios;
    private final ObjecionRepository objeciones;
    private final ResultadoCicloRepository resultados;
    private final SegundaCitaRepository citas;
    private final MejoraService mejoras;
    private final MaquinaEstados maquina;
    private final TinderProperties props;

    public CasamientoService(CasamientoRepository casamientos, ProcedimientoRepository procedimientos,
                             UsuarioRepository usuarios, ObjecionRepository objeciones, ResultadoCicloRepository resultados,
                             SegundaCitaRepository citas, MejoraService mejoras, MaquinaEstados maquina,
                             TinderProperties props) {
        this.casamientos = casamientos;
        this.procedimientos = procedimientos;
        this.usuarios = usuarios;
        this.objeciones = objeciones;
        this.resultados = resultados;
        this.citas = citas;
        this.mejoras = mejoras;
        this.maquina = maquina;
        this.props = props;
    }

    /**
     * Requisitos: PENDIENTE_CASAMIENTO, ninguna objeción fundada sin respuesta, votos y versión express
     * (validados en el request) y oficiante con rol JUEZ_PAZ.
     */
    @Transactional
    public CasamientoDTO casar(CasamientoRequest req) {
        Procedimiento p = procedimientos.findById(req.procedimientoId())
                .orElseThrow(() -> ApiException.noEncontrado("No existe el procedimiento " + req.procedimientoId()));
        if (p.getEstado() != EstadoProcedimiento.PENDIENTE_CASAMIENTO) {
            throw ApiException.reglaNegocio("TRANSICION_INVALIDA", p.getCodigo()
                    + " no está pendiente de casamiento (" + MaquinaEstados.legible(p.getEstado())
                    + "). Solo se casan los que llegaron al 70 %.");
        }
        if (objeciones.existsByProcedimientoIdAndFundadaTrueAndRespuestaIsNull(p.getId())) {
            throw ApiException.reglaNegocio("OBJECION_SIN_RESPUESTA", p.getCodigo()
                    + " tiene objeciones fundadas sin respuesta: nadie se casa con cuentas pendientes.");
        }
        Usuario oficiante = usuario(req.oficianteId());
        if (oficiante.getRol() != Rol.JUEZ_PAZ) {
            throw ApiException.reglaNegocio("OFICIANTE_INVALIDO", "El casamiento lo oficia el juez de paz ("
                    + oficiante.getNombre() + " no lo es). La Dirección firma y revisa, no oficia.");
        }
        Usuario padrino = req.padrinoId() != null ? usuario(req.padrinoId()) : p.getAutor();
        Usuario dueno = req.duenoId() != null ? usuario(req.duenoId()) : p.getDueno();

        Set<Area> areas = new LinkedHashSet<>(p.getAreas());
        if (req.areaIds() != null && !req.areaIds().isEmpty()) {
            areas.removeIf(a -> !req.areaIds().contains(a.getId()));
            if (areas.size() != req.areaIds().size()) {
                throw ApiException.reglaNegocio("AREA_INVALIDA", "Solo se pueden casar áreas que comparten el procedimiento.");
            }
        }
        Set<Long> areaIds = areas.stream().map(Area::getId).collect(java.util.stream.Collectors.toSet());
        Set<Usuario> testigos = new LinkedHashSet<>();
        for (Long id : Optional.ofNullable(req.testigoIds()).orElse(Set.of())) {
            Usuario t = usuario(id);
            if (areaIds.contains(t.getArea().getId())) {
                throw ApiException.reglaNegocio("TESTIGO_INVALIDO", t.getNombre()
                        + " es de un área que se casa: los testigos son de OTRAS áreas que reciben el trabajo.");
            }
            testigos.add(t);
        }

        String condiciones = blancoANull(req.condiciones());
        if (condiciones == null) {
            condiciones = citas.findFirstByProcedimientoIdAndDecisionOrderByIdDesc(p.getId(),
                    DecisionSegundaCita.CASAR_CON_CONDICIONES).map(SegundaCita::getCondiciones).orElse(null);
        }
        List<CasamientoResultado> foto = resultados.ultimosDelProcedimiento(p.getId()).stream()
                .filter(r -> r.getVeredicto() != Veredicto.QUORUM_INSUFICIENTE && areaIds.contains(r.getArea().getId()))
                .map(r -> new CasamientoResultado(r.getArea(), r.getPctLike()))
                .toList();

        LocalDate fecha = req.fecha() != null ? req.fecha() : LocalDate.now();
        p.setVersionExpress(req.versionExpress().trim());
        maquina.transicionar(p, EstadoProcedimiento.CASADO);
        Casamiento c = casamientos.save(new Casamiento(siguienteNumero(fecha), p, fecha, req.votos().trim(), padrino,
                dueno, oficiante, areas, testigos, foto, condiciones, fecha.plusMonths(props.mesesAniversario())));
        return dto(c);
    }

    private String siguienteNumero(LocalDate fecha) {
        String anio = String.valueOf(fecha.getYear());
        return "L-%03d/%s".formatted(casamientos.ultimoNumeroDelAnio(anio) + 1, anio);
    }

    @Transactional(readOnly = true)
    public List<CasamientoDTO> listar() {
        return casamientos.findAllByOrderByFechaDescIdDesc().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public CasamientoDTO obtener(Long id) {
        return dto(buscar(id));
    }

    /** Informe de los casamientos vigentes con aniversario vencido o dentro de los próximos N días. */
    @Transactional(readOnly = true)
    public List<AniversarioDTO> aniversarios() {
        return casamientos.findByEstadoAndFechaAniversarioLessThanEqualOrderByFechaAniversario(
                        EstadoCasamiento.VIGENTE, LocalDate.now().plusDays(props.diasAvisoAniversario()))
                .stream()
                .map(c -> new AniversarioDTO(dto(c),
                        objeciones.countByProcedimientoIdAndEstadoNot(c.getProcedimiento().getId(), EstadoObjecion.RESPONDIDA),
                        mejoras.abiertasDe(c.getProcedimiento().getId())))
                .toList();
    }

    @Transactional
    public CasamientoDTO renovar(Long id, String usoReal) {
        Casamiento c = vigente(id);
        c.setInformeAniversario(blancoANull(usoReal));
        c.setEstado(EstadoCasamiento.RENOVADO);
        Procedimiento p = c.getProcedimiento();
        LocalDate hoy = LocalDate.now();
        Casamiento nuevo = casamientos.save(new Casamiento(siguienteNumero(hoy), p, hoy, c.getVotos(), c.getPadrino(),
                p.getDueno() != null ? p.getDueno() : c.getDueno(), c.getOficiante(), c.getAreas(), c.getTestigos(),
                c.getResultados(), c.getCondiciones(), hoy.plusMonths(props.mesesAniversario())));
        return dto(nuevo);
    }

    /** El aniversario manda el procedimiento a una segunda cita (queda una mejora con el uso real). */
    @Transactional
    public CasamientoDTO segundaCita(Long id, String usoReal, UsuarioAutenticado juez) {
        Casamiento c = vigente(id);
        String informe = blancoANull(usoReal);
        c.setInformeAniversario(informe);
        c.setEstado(EstadoCasamiento.POSTERGADO);
        Procedimiento p = c.getProcedimiento();
        maquina.transicionar(p, EstadoProcedimiento.SEGUNDA_CITA);
        citas.save(new SegundaCita(p, null, LocalDate.now().plusDays(props.diasHastaSegundaCita())));
        mejoras.crearAutomatica(OrigenMejora.ANIVERSARIO, String.valueOf(c.getId()), p,
                "Revisar " + p.getCodigo() + " en segunda cita tras el aniversario"
                        + (informe != null ? ". Uso real: " + informe : "."), juez.id(), null);
        return dto(c);
    }

    /** Divorcio: PENDIENTE_VELORIO, o SEGUNDA_CITA si es obligatorio por norma. */
    @Transactional
    public CasamientoDTO divorciar(Long id, String usoReal) {
        Casamiento c = vigente(id);
        c.setInformeAniversario(blancoANull(usoReal));
        c.setEstado(EstadoCasamiento.DIVORCIADO);
        Procedimiento p = c.getProcedimiento();
        if (p.isObligatorio()) {
            maquina.transicionar(p, EstadoProcedimiento.SEGUNDA_CITA);
            citas.save(new SegundaCita(p, null, LocalDate.now().plusDays(props.diasHastaSegundaCita())));
        } else {
            maquina.transicionar(p, EstadoProcedimiento.PENDIENTE_VELORIO);
        }
        return dto(c);
    }

    private CasamientoDTO dto(Casamiento c) {
        List<Objecion> tratadas = objeciones.findByProcedimientoId(c.getProcedimiento().getId()).stream()
                .filter(o -> o.getEstado() != EstadoObjecion.PENDIENTE)
                .sorted(Comparator.comparing(Objecion::getCategoria))
                .toList();
        return Mapeador.casamiento(c, tratadas);
    }

    private Casamiento vigente(Long id) {
        Casamiento c = buscar(id);
        if (c.getEstado() != EstadoCasamiento.VIGENTE) {
            throw ApiException.reglaNegocio("CASAMIENTO_NO_VIGENTE",
                    "Ese casamiento ya está " + c.getEstado().name().toLowerCase() + ".");
        }
        return c;
    }

    private Usuario usuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el usuario " + id));
    }

    private Casamiento buscar(Long id) {
        return casamientos.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el casamiento " + id));
    }
}
