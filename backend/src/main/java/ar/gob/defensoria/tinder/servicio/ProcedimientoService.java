package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.Area;
import ar.gob.defensoria.tinder.dominio.EstadoProcedimiento;
import ar.gob.defensoria.tinder.dominio.Procedimiento;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.ProcedimientoDTO;
import ar.gob.defensoria.tinder.web.dto.ProcedimientoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProcedimientoService {

    private final ProcedimientoRepository procedimientos;
    private final AreaRepository areas;
    private final UsuarioRepository usuarios;
    private final ParticipacionRepository participaciones;
    private final ResultadoCicloRepository resultados;

    public ProcedimientoService(ProcedimientoRepository procedimientos, AreaRepository areas, UsuarioRepository usuarios,
                                ParticipacionRepository participaciones, ResultadoCicloRepository resultados) {
        this.procedimientos = procedimientos;
        this.areas = areas;
        this.usuarios = usuarios;
        this.participaciones = participaciones;
        this.resultados = resultados;
    }

    @Transactional(readOnly = true)
    public List<ProcedimientoDTO> listar(EstadoProcedimiento estado) {
        List<Procedimiento> lista = estado == null
                ? procedimientos.findAllByOrderByCodigo()
                : procedimientos.findByEstadoOrderByCodigo(estado);
        return lista.stream().map(Mapeador::procedimiento).toList();
    }

    @Transactional(readOnly = true)
    public ProcedimientoDTO obtener(Long id) {
        return Mapeador.procedimiento(buscar(id));
    }

    @Transactional
    public ProcedimientoDTO crear(ProcedimientoRequest req) {
        if (procedimientos.existsByCodigoIgnoreCase(req.codigo().trim())) {
            throw ApiException.conflicto("CODIGO_DUPLICADO", "Ya existe un procedimiento con el código " + req.codigo());
        }
        Procedimiento p = new Procedimiento();
        aplicar(p, req);
        return Mapeador.procedimiento(procedimientos.save(p));
    }

    @Transactional
    public ProcedimientoDTO actualizar(Long id, ProcedimientoRequest req) {
        Procedimiento p = buscar(id);
        if (p.getEstado() == EstadoProcedimiento.VELADO) {
            throw ApiException.reglaNegocio("PROCEDIMIENTO_VELADO",
                    p.getCodigo() + " ya fue velado: es un documento obsoleto y no se edita (ISO 9001 7.5.3).");
        }
        if (!p.getCodigo().equalsIgnoreCase(req.codigo().trim()) && procedimientos.existsByCodigoIgnoreCase(req.codigo().trim())) {
            throw ApiException.conflicto("CODIGO_DUPLICADO", "Ya existe un procedimiento con el código " + req.codigo());
        }
        if (req.obligatorio() && p.getEstado() == EstadoProcedimiento.PENDIENTE_VELORIO) {
            throw ApiException.reglaNegocio("TRANSICION_INVALIDA",
                    p.getCodigo() + " está pendiente de velorio: no se puede volver obligatorio a último momento.");
        }
        aplicar(p, req);
        return Mapeador.procedimiento(p);
    }

    /**
     * Solo se borra físicamente un procedimiento que nunca entró en juego (sin votos ni resultados).
     * Todo lo demás se da de baja por velorio: la información documentada no se destruye.
     */
    @Transactional
    public void eliminar(Long id) {
        Procedimiento p = buscar(id);
        if (p.getEstado() != EstadoProcedimiento.EN_TINDER
                || participaciones.existsByIdProcedimientoId(id)
                || resultados.existsByProcedimientoId(id)) {
            throw ApiException.reglaNegocio("NO_SE_BORRA",
                    p.getCodigo() + " ya tiene historia: los procedimientos no se borran, se velan (ISO 9001 7.5.3).");
        }
        procedimientos.delete(p);
    }

    private void aplicar(Procedimiento p, ProcedimientoRequest req) {
        if (req.obligatorio() && (req.baseObligatoriedad() == null || req.baseObligatoriedad().isBlank())) {
            throw ApiException.reglaNegocio("FALTA_BASE_NORMATIVA",
                    "Si es obligatorio, indicá la base (ej. \"ISO 9001 7.5.3\" o la ley que corresponda).");
        }
        Set<Area> nuevasAreas = new LinkedHashSet<>(areas.findAllById(req.areaIds()));
        if (nuevasAreas.size() != req.areaIds().size()) {
            throw ApiException.noEncontrado("Alguna de las áreas indicadas no existe.");
        }
        p.setCodigo(req.codigo().trim().toUpperCase());
        p.setNombre(req.nombre().trim());
        p.setTipo(req.tipo());
        p.setBio(req.bio().trim());
        p.setFechaCreacion(req.fechaCreacion());
        p.setMinutosPorUso(req.minutosPorUso());
        p.setUsosPorSemana(req.usosPorSemana());
        p.setBeneficio(req.beneficio().trim());
        p.setObligatorio(req.obligatorio());
        p.setBaseObligatoriedad(blancoANull(req.baseObligatoriedad()));
        p.setVersionExpress(blancoANull(req.versionExpress()));
        p.setAutor(req.autorId() == null ? null : usuarios.findById(req.autorId())
                .orElseThrow(() -> ApiException.noEncontrado("No existe el usuario " + req.autorId())));
        p.setAreas(nuevasAreas);
    }

    private Procedimiento buscar(Long id) {
        return procedimientos.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el procedimiento " + id));
    }

    static String blancoANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
