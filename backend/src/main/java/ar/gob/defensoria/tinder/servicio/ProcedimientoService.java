package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.ArchivoDTO;
import ar.gob.defensoria.tinder.web.dto.ContenidoRequest;
import ar.gob.defensoria.tinder.web.dto.ProcedimientoDTO;
import ar.gob.defensoria.tinder.web.dto.ProcedimientoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ProcedimientoService {

    private final ProcedimientoRepository procedimientos;
    private final AreaRepository areas;
    private final UsuarioRepository usuarios;
    private final VotoEmitidoRepository votosEmitidos;
    private final ResultadoCicloRepository resultados;
    private final MatrizParticipacionRepository matriz;
    private final SegundaCitaRepository citas;
    private final VelorioRepository velorios;
    private final MaquinaEstados maquina;

    public ProcedimientoService(ProcedimientoRepository procedimientos, AreaRepository areas, UsuarioRepository usuarios,
                                VotoEmitidoRepository votosEmitidos, ResultadoCicloRepository resultados,
                                MatrizParticipacionRepository matriz, SegundaCitaRepository citas,
                                VelorioRepository velorios, MaquinaEstados maquina) {
        this.procedimientos = procedimientos;
        this.areas = areas;
        this.usuarios = usuarios;
        this.votosEmitidos = votosEmitidos;
        this.resultados = resultados;
        this.matriz = matriz;
        this.citas = citas;
        this.velorios = velorios;
        this.maquina = maquina;
    }

    /** Sin estado: todos menos los VELADOS (esos solo se ven en el archivo de obsoletos). */
    @Transactional(readOnly = true)
    public List<ProcedimientoDTO> listar(EstadoProcedimiento estado) {
        List<Procedimiento> lista = estado == null
                ? procedimientos.findByEstadoNotOrderByCodigo(EstadoProcedimiento.VELADO)
                : procedimientos.findByEstadoOrderByCodigo(estado);
        return lista.stream().map(Mapeador::procedimiento).toList();
    }

    @Transactional(readOnly = true)
    public ProcedimientoDTO obtener(Long id) {
        return Mapeador.procedimiento(buscar(id));
    }

    /** Archivo de obsoletos: los velados con su acta (solo lectura). */
    @Transactional(readOnly = true)
    public List<ArchivoDTO> archivo() {
        return procedimientos.findByEstadoOrderByCodigo(EstadoProcedimiento.VELADO).stream()
                .map(p -> {
                    var v = velorios.findByProcedimientoId(p.getId());
                    var r = v.map(Velorio::getReencarnacion).orElse(null);
                    return new ArchivoDTO(Mapeador.procedimiento(p), v.map(Velorio::getId).orElse(null),
                            v.map(Velorio::getNumeroActa).orElse(null), v.map(Velorio::getFecha).orElse(null),
                            r != null ? r.getCodigo() : null, r != null ? r.getNombre() : null);
                })
                .toList();
    }

    /** Procedimientos de los que el usuario es dueño, con la marca de si los puede editar ahora. */
    @Transactional(readOnly = true)
    public List<ProcedimientoDTO> mios(UsuarioAutenticado usuario) {
        return procedimientos.findByDuenoIdOrderByCodigo(usuario.id()).stream()
                .filter(p -> p.getEstado() != EstadoProcedimiento.VELADO)
                .map(p -> Mapeador.procedimiento(p, editablePorDueno(p)))
                .toList();
    }

    /** El dueño edita tras una segunda cita con AJUSTAR_Y_REVOTAR (mientras siga en segunda cita) o si es un borrador. */
    boolean editablePorDueno(Procedimiento p) {
        if (p.getEstado() == EstadoProcedimiento.BORRADOR) {
            return true;
        }
        return p.getEstado() == EstadoProcedimiento.SEGUNDA_CITA
                && citas.findFirstByProcedimientoIdAndEstadoOrderByIdDesc(p.getId(), EstadoSegundaCita.REALIZADA)
                .map(c -> c.getDecision() == DecisionSegundaCita.AJUSTAR_Y_REVOTAR).orElse(false);
    }

    /** Alta (juez de paz). La matriz arranca con la regla vieja: toda la gente de sus áreas. */
    @Transactional
    public ProcedimientoDTO crear(ProcedimientoRequest req) {
        if (procedimientos.existsByCodigoIgnoreCase(req.codigo().trim())) {
            throw ApiException.conflicto("CODIGO_DUPLICADO", "Ya existe un procedimiento con el código " + req.codigo());
        }
        Procedimiento p = new Procedimiento();
        aplicar(p, req);
        Procedimiento guardado = procedimientos.save(p);
        LocalDate hoy = LocalDate.now();
        for (Area a : guardado.getAreas()) {
            usuarios.findByAreaIdOrderByNombre(a.getId()).stream()
                    .filter(u -> u.getRol() != Rol.DIRECCION)
                    .forEach(u -> matriz.save(new MatrizParticipacion(u.getId(), guardado.getId(), hoy)));
        }
        return Mapeador.procedimiento(guardado);
    }

    /** Edición completa (juez de paz). Si cambia el contenido, sube la versión. */
    @Transactional
    public ProcedimientoDTO actualizar(Long id, ProcedimientoRequest req) {
        Procedimiento p = buscar(id);
        noVelado(p);
        if (!p.getCodigo().equalsIgnoreCase(req.codigo().trim()) && procedimientos.existsByCodigoIgnoreCase(req.codigo().trim())) {
            throw ApiException.conflicto("CODIGO_DUPLICADO", "Ya existe un procedimiento con el código " + req.codigo());
        }
        if (req.obligatorio() && p.getEstado() == EstadoProcedimiento.PENDIENTE_VELORIO) {
            throw ApiException.reglaNegocio("TRANSICION_INVALIDA",
                    p.getCodigo() + " está pendiente de velorio: no se puede volver obligatorio a último momento.");
        }
        String antes = huella(p);
        aplicar(p, req);
        subirVersionSiCambio(p, antes);
        return Mapeador.procedimiento(p);
    }

    /** Edición del contenido por el dueño (o el juez). Cada cambio sube la versión. */
    @Transactional
    public ProcedimientoDTO editarContenido(Long id, ContenidoRequest req, UsuarioAutenticado usuario) {
        Procedimiento p = buscar(id);
        noVelado(p);
        boolean esDueno = p.getDueno() != null && p.getDueno().getId().equals(usuario.id());
        if (usuario.rol() != Rol.JUEZ_PAZ && !(esDueno && editablePorDueno(p))) {
            throw ApiException.prohibido(esDueno
                    ? "Podés editar " + p.getCodigo() + " después de una segunda cita que decida ajustarlo."
                    : "Solo el dueño de " + p.getCodigo() + " (o el juez de paz) edita su contenido.");
        }
        String antes = huella(p);
        p.setNombre(req.nombre().trim());
        p.setBio(req.bio().trim());
        p.setMinutosPorUso(req.minutosPorUso());
        p.setUsosPorSemana(req.usosPorSemana());
        p.setBeneficio(req.beneficio().trim());
        p.setVersionExpress(blancoANull(req.versionExpress()));
        subirVersionSiCambio(p, antes);
        return Mapeador.procedimiento(p, editablePorDueno(p));
    }

    /** BORRADOR → EN_TINDER: entra al mazo del ciclo trimestral abierto o del próximo. */
    @Transactional
    public ProcedimientoDTO publicar(Long id) {
        Procedimiento p = buscar(id);
        if (p.getEstado() != EstadoProcedimiento.BORRADOR) {
            throw ApiException.reglaNegocio("TRANSICION_INVALIDA", p.getCodigo() + " no es un borrador ("
                    + MaquinaEstados.legible(p.getEstado()) + ").");
        }
        maquina.transicionar(p, EstadoProcedimiento.EN_TINDER);
        return Mapeador.procedimiento(p);
    }

    /**
     * Solo se borra físicamente un procedimiento que nunca entró en juego (sin votos ni resultados).
     * Todo lo demás se da de baja por velorio: la información documentada no se destruye.
     */
    @Transactional
    public void eliminar(Long id) {
        Procedimiento p = buscar(id);
        boolean sinHistoria = p.getEstado() == EstadoProcedimiento.EN_TINDER || p.getEstado() == EstadoProcedimiento.BORRADOR;
        if (!sinHistoria || votosEmitidos.existsByIdProcedimientoId(id) || resultados.existsByProcedimientoId(id)
                || p.getReemplazaA() != null) {
            throw ApiException.reglaNegocio("NO_SE_BORRA",
                    p.getCodigo() + " ya tiene historia: los procedimientos no se borran, se velan (ISO 9001 7.5.3).");
        }
        matriz.borrarDelProcedimiento(id);
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
        p.setAutor(req.autorId() == null ? null : usuario(req.autorId()));
        p.setDueno(req.duenoId() == null ? null : usuario(req.duenoId()));
        p.setAreas(nuevasAreas);
    }

    /** Lo que cuenta como "contenido": si cambia, es una versión nueva. */
    private static String huella(Procedimiento p) {
        return String.join("\u0000", Objects.toString(p.getNombre(), ""), Objects.toString(p.getBio(), ""),
                String.valueOf(p.getMinutosPorUso()), String.valueOf(p.getUsosPorSemana()),
                Objects.toString(p.getBeneficio(), ""), Objects.toString(p.getVersionExpress(), ""));
    }

    private static void subirVersionSiCambio(Procedimiento p, String huellaAnterior) {
        if (!huella(p).equals(huellaAnterior)) {
            p.setNumeroVersion(p.getNumeroVersion() + 1);
        }
    }

    private static void noVelado(Procedimiento p) {
        if (p.getEstado() == EstadoProcedimiento.VELADO) {
            throw ApiException.reglaNegocio("PROCEDIMIENTO_VELADO",
                    p.getCodigo() + " ya fue velado: es un documento obsoleto y no se edita (ISO 9001 7.5.3).");
        }
    }

    private Usuario usuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el usuario " + id));
    }

    private Procedimiento buscar(Long id) {
        return procedimientos.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el procedimiento " + id));
    }

    static String blancoANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
