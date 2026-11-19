package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.MatrizParticipacionRepository;
import ar.gob.defensoria.tinder.repositorio.MatrizRevisionRepository;
import ar.gob.defensoria.tinder.repositorio.ProcedimientoRepository;
import ar.gob.defensoria.tinder.repositorio.UsuarioRepository;
import ar.gob.defensoria.tinder.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Matriz de participación: quién usa o ejecuta cada procedimiento (y por lo tanto lo vota). */
@Service
public class MatrizService {

    private final MatrizParticipacionRepository matriz;
    private final MatrizRevisionRepository revisiones;
    private final UsuarioRepository usuarios;
    private final ProcedimientoRepository procedimientos;

    public MatrizService(MatrizParticipacionRepository matriz, MatrizRevisionRepository revisiones,
                         UsuarioRepository usuarios, ProcedimientoRepository procedimientos) {
        this.matriz = matriz;
        this.revisiones = revisiones;
        this.usuarios = usuarios;
        this.procedimientos = procedimientos;
    }

    /** Grilla usuario × procedimiento (sin la Dirección, que no vota, y sin los velados). */
    @Transactional(readOnly = true)
    public MatrizDTO grilla(Long areaId) {
        List<Usuario> lista = (areaId == null ? usuarios.findAllByOrderByNombre() : usuarios.findByAreaIdOrderByNombre(areaId))
                .stream().filter(u -> u.getRol() != Rol.DIRECCION).toList();
        Set<Long> ids = lista.stream().map(Usuario::getId).collect(Collectors.toSet());
        List<MatrizDTO.Proc> procs = procedimientos.findByEstadoNotOrderByCodigo(EstadoProcedimiento.VELADO).stream()
                .map(p -> new MatrizDTO.Proc(p.getId(), p.getCodigo(), p.getNombre(), p.getEstado(),
                        p.getAreas().stream().map(Area::getNombre).toList()))
                .toList();
        List<CeldaDTO> celdas = matriz.findAll().stream()
                .filter(m -> ids.contains(m.getId().usuarioId()))
                .map(m -> new CeldaDTO(m.getId().usuarioId(), m.getId().procedimientoId()))
                .toList();
        return new MatrizDTO(lista.stream().map(UsuarioDTO::de).toList(), procs, celdas);
    }

    /** Guardado por lote: altas y bajas en una sola transacción. Queda registrado en la bitácora. */
    @Transactional
    public MatrizLoteDTO guardar(MatrizLoteRequest req, UsuarioAutenticado juez) {
        List<CeldaDTO> altas = Optional.ofNullable(req.altas()).orElse(List.of());
        List<CeldaDTO> bajas = Optional.ofNullable(req.bajas()).orElse(List.of());
        Set<Long> usuariosIds = new HashSet<>();
        Set<Long> procIds = new HashSet<>();
        altas.forEach(c -> { usuariosIds.add(c.usuarioId()); procIds.add(c.procedimientoId()); });
        Map<Long, Usuario> porId = usuarios.findAllById(usuariosIds).stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u));
        if (porId.size() != usuariosIds.size()) {
            throw ApiException.noEncontrado("Algún usuario de la matriz no existe.");
        }
        if (porId.values().stream().anyMatch(u -> u.getRol() == Rol.DIRECCION)) {
            throw ApiException.reglaNegocio("DIRECCION_NO_VOTA", "La Dirección es padrino institucional: no entra en la matriz.");
        }
        Map<Long, Procedimiento> procs = procedimientos.findAllById(procIds).stream()
                .collect(Collectors.toMap(Procedimiento::getId, p -> p));
        if (procs.size() != procIds.size()) {
            throw ApiException.noEncontrado("Algún procedimiento de la matriz no existe.");
        }
        if (procs.values().stream().anyMatch(p -> p.getEstado() == EstadoProcedimiento.VELADO)) {
            throw ApiException.reglaNegocio("PROCEDIMIENTO_VELADO", "Los velados no se usan más: no van en la matriz.");
        }

        LocalDate hoy = LocalDate.now();
        int nAltas = 0;
        for (CeldaDTO c : new LinkedHashSet<>(altas)) {
            var id = new MatrizParticipacion.Id(c.usuarioId(), c.procedimientoId());
            if (!matriz.existsById(id)) {
                matriz.save(new MatrizParticipacion(c.usuarioId(), c.procedimientoId(), hoy));
                nAltas++;
            }
        }
        int nBajas = 0;
        for (CeldaDTO c : new LinkedHashSet<>(bajas)) {
            var id = new MatrizParticipacion.Id(c.usuarioId(), c.procedimientoId());
            if (matriz.existsById(id)) {
                matriz.deleteById(id);
                nBajas++;
            }
        }
        revisiones.save(new MatrizRevision(hoy, juez.id(), nAltas, nBajas));
        String mensaje = nAltas + nBajas == 0
                ? "Matriz revisada: estaba perfecta, no hubo cambios."
                : "Matriz guardada: " + nAltas + (nAltas == 1 ? " alta" : " altas") + " y " + nBajas
                + (nBajas == 1 ? " baja." : " bajas.");
        return new MatrizLoteDTO(nAltas, nBajas, mensaje);
    }
}
