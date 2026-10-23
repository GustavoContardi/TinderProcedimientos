package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.TinderProperties;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.CasamientoRepository;
import ar.gob.defensoria.tinder.repositorio.ProcedimientoRepository;
import ar.gob.defensoria.tinder.repositorio.UsuarioRepository;
import ar.gob.defensoria.tinder.web.dto.CasamientoDTO;
import ar.gob.defensoria.tinder.web.dto.CasamientoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CasamientoService {

    private final CasamientoRepository casamientos;
    private final ProcedimientoRepository procedimientos;
    private final UsuarioRepository usuarios;
    private final MaquinaEstados maquina;
    private final TinderProperties props;

    public CasamientoService(CasamientoRepository casamientos, ProcedimientoRepository procedimientos,
                             UsuarioRepository usuarios, MaquinaEstados maquina, TinderProperties props) {
        this.casamientos = casamientos;
        this.procedimientos = procedimientos;
        this.usuarios = usuarios;
        this.maquina = maquina;
        this.props = props;
    }

    @Transactional
    public CasamientoDTO casar(CasamientoRequest req) {
        Procedimiento p = procedimientos.findById(req.procedimientoId())
                .orElseThrow(() -> ApiException.noEncontrado("No existe el procedimiento " + req.procedimientoId()));
        if (p.getEstado() != EstadoProcedimiento.PENDIENTE_CASAMIENTO) {
            throw ApiException.reglaNegocio("TRANSICION_INVALIDA", p.getCodigo()
                    + " no está pendiente de casamiento (" + MaquinaEstados.legible(p.getEstado())
                    + "). Solo se casan los que hicieron match.");
        }
        Usuario oficiante = usuarios.findById(req.oficianteId())
                .orElseThrow(() -> ApiException.noEncontrado("No existe el usuario " + req.oficianteId()));
        if (oficiante.getRol() != Rol.DIRECCION) {
            throw ApiException.reglaNegocio("OFICIANTE_INVALIDO", "El casamiento lo tiene que oficiar alguien de la Dirección.");
        }
        Usuario padrino = req.padrinoId() != null
                ? usuarios.findById(req.padrinoId()).orElseThrow(() -> ApiException.noEncontrado("No existe el padrino " + req.padrinoId()))
                : p.getAutor();

        Set<Area> areas = new LinkedHashSet<>(p.getAreas());
        if (req.areaIds() != null && !req.areaIds().isEmpty()) {
            areas.removeIf(a -> !req.areaIds().contains(a.getId()));
            if (areas.size() != req.areaIds().size()) {
                throw ApiException.reglaNegocio("AREA_INVALIDA", "Solo se pueden casar áreas que comparten el procedimiento.");
            }
        }

        LocalDate fecha = req.fecha() != null ? req.fecha() : LocalDate.now();
        p.setVersionExpress(req.versionExpress().trim());
        maquina.transicionar(p, EstadoProcedimiento.CASADO);
        Casamiento c = casamientos.save(new Casamiento(p, fecha, req.votos().trim(), padrino, oficiante, areas,
                fecha.plusMonths(props.mesesAniversario())));
        return Mapeador.casamiento(c);
    }

    @Transactional(readOnly = true)
    public List<CasamientoDTO> listar() {
        return casamientos.findAllByOrderByFechaDescIdDesc().stream().map(Mapeador::casamiento).toList();
    }

    @Transactional(readOnly = true)
    public CasamientoDTO obtener(Long id) {
        return Mapeador.casamiento(buscar(id));
    }

    /** Casamientos vigentes con aniversario vencido o dentro de los próximos N días (default 15). */
    @Transactional(readOnly = true)
    public List<CasamientoDTO> aniversarios() {
        return casamientos.findByEstadoAndFechaAniversarioLessThanEqualOrderByFechaAniversario(
                        EstadoCasamiento.VIGENTE, LocalDate.now().plusDays(props.diasAvisoAniversario()))
                .stream().map(Mapeador::casamiento).toList();
    }

    @Transactional
    public CasamientoDTO renovar(Long id) {
        Casamiento c = vigente(id);
        c.setEstado(EstadoCasamiento.RENOVADO);
        LocalDate hoy = LocalDate.now();
        Casamiento nuevo = casamientos.save(new Casamiento(c.getProcedimiento(), hoy, c.getVotos(), c.getPadrino(),
                c.getOficiante(), c.getAreas(), hoy.plusMonths(props.mesesAniversario())));
        return Mapeador.casamiento(nuevo);
    }

    @Transactional
    public CasamientoDTO divorciar(Long id) {
        Casamiento c = vigente(id);
        c.setEstado(EstadoCasamiento.DIVORCIADO);
        Procedimiento p = c.getProcedimiento();
        maquina.transicionar(p, p.isObligatorio() ? EstadoProcedimiento.SEGUNDA_CITA : EstadoProcedimiento.PENDIENTE_VELORIO);
        return Mapeador.casamiento(c);
    }

    private Casamiento vigente(Long id) {
        Casamiento c = buscar(id);
        if (c.getEstado() != EstadoCasamiento.VIGENTE) {
            throw ApiException.reglaNegocio("CASAMIENTO_NO_VIGENTE",
                    "Ese casamiento ya está " + c.getEstado().name().toLowerCase() + ".");
        }
        return c;
    }

    private Casamiento buscar(Long id) {
        return casamientos.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el casamiento " + id));
    }
}
