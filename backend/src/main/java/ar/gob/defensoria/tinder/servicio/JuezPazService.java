package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.CalendarioDTO;
import ar.gob.defensoria.tinder.web.dto.CicloDTO;
import ar.gob.defensoria.tinder.web.dto.DesignacionDTO;
import ar.gob.defensoria.tinder.web.dto.DesignacionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Rotación del juez de paz (la designa la Dirección) y calendario trimestral calculado. */
@Service
public class JuezPazService {

    private static final DateTimeFormatter DDMM = DateTimeFormatter.ofPattern("d/M");

    private final DesignacionJuezRepository designaciones;
    private final UsuarioRepository usuarios;
    private final AreaRepository areas;
    private final CicloVotacionRepository ciclos;
    private final MatrizRevisionRepository revisiones;
    private final MatrizParticipacionRepository matriz;
    private final VotoEmitidoRepository votosEmitidos;
    private final ResultadoProcedimientoRepository resultadosProc;
    private final SegundaCitaRepository citas;
    private final ProcedimientoRepository procedimientos;
    private final VelorioRepository velorios;
    private final MejoraRepository mejoras;

    public JuezPazService(DesignacionJuezRepository designaciones, UsuarioRepository usuarios, AreaRepository areas,
                          CicloVotacionRepository ciclos, MatrizRevisionRepository revisiones,
                          MatrizParticipacionRepository matriz, VotoEmitidoRepository votosEmitidos,
                          ResultadoProcedimientoRepository resultadosProc, SegundaCitaRepository citas,
                          ProcedimientoRepository procedimientos, VelorioRepository velorios, MejoraRepository mejoras) {
        this.designaciones = designaciones;
        this.usuarios = usuarios;
        this.areas = areas;
        this.ciclos = ciclos;
        this.revisiones = revisiones;
        this.matriz = matriz;
        this.votosEmitidos = votosEmitidos;
        this.resultadosProc = resultadosProc;
        this.citas = citas;
        this.procedimientos = procedimientos;
        this.velorios = velorios;
        this.mejoras = mejoras;
    }

    @Transactional(readOnly = true)
    public List<DesignacionDTO> historial() {
        return designaciones.findAllByOrderByDesdeDescIdDesc().stream().map(DesignacionDTO::de).toList();
    }

    /**
     * Cierra la designación vigente del área (hasta = hoy), crea la nueva, asigna JUEZ_PAZ al nuevo
     * y devuelve a EMPLEADO al anterior.
     */
    @Transactional
    public DesignacionDTO designar(DesignacionRequest req) {
        Usuario nuevo = usuarios.findById(req.usuarioId())
                .orElseThrow(() -> ApiException.noEncontrado("No existe el usuario " + req.usuarioId()));
        Area area = areas.findById(req.areaId())
                .orElseThrow(() -> ApiException.noEncontrado("No existe el área " + req.areaId()));
        if (nuevo.getRol() == Rol.DIRECCION) {
            throw ApiException.reglaNegocio("DESIGNACION_INVALIDA",
                    "La Dirección es padrino institucional: designa al juez, no puede serlo.");
        }
        if (!nuevo.getArea().getId().equals(area.getId())) {
            throw ApiException.reglaNegocio("DESIGNACION_INVALIDA", nuevo.getNombre() + " no es de " + area.getNombre()
                    + ": el juez de paz se elige entre la gente del área.");
        }
        LocalDate hoy = LocalDate.now();
        Optional<DesignacionJuez> vigente = designaciones.findFirstByAreaIdAndHastaIsNull(area.getId());
        if (vigente.isPresent()) {
            DesignacionJuez actual = vigente.get();
            if (actual.getUsuario().getId().equals(nuevo.getId())) {
                throw ApiException.conflicto("YA_ES_JUEZ", nuevo.getNombre() + " ya es el juez de paz de " + area.getNombre() + ".");
            }
            actual.cerrar(hoy);
            designaciones.saveAndFlush(actual);
            Usuario anterior = actual.getUsuario();
            if (!designaciones.existsByUsuarioIdAndHastaIsNull(anterior.getId())) {
                anterior.setRol(Rol.EMPLEADO);
            }
        }
        nuevo.setRol(Rol.JUEZ_PAZ);
        return DesignacionDTO.de(designaciones.save(new DesignacionJuez(nuevo, area, hoy)));
    }

    /** Checklist del trimestre (ciclo trimestral abierto o, si no hay, el último). */
    @Transactional(readOnly = true)
    public CalendarioDTO calendario() {
        Optional<CicloVotacion> opt = ciclos.findFirstByEstadoAndTipo(EstadoCiclo.ABIERTO, TipoCiclo.TRIMESTRAL)
                .or(() -> ciclos.findFirstByTipoOrderByFechaInicioDesc(TipoCiclo.TRIMESTRAL));
        List<CalendarioDTO.Item> items = new ArrayList<>();
        CicloVotacion ciclo = opt.orElse(null);
        LocalDate hoy = LocalDate.now();

        LocalDate inicio = ciclo != null ? ciclo.getFechaInicio() : hoy;
        Optional<MatrizRevision> rev = revisiones.findFirstByFechaBetweenOrderByFechaDesc(inicio.minusDays(30), inicio.plusDays(7));
        items.add(new CalendarioDTO.Item("Semana 1", "Matriz revisada",
                rev.map(r -> "Revisada el " + r.getFecha().format(DDMM) + " (" + r.getAltas() + " altas, " + r.getBajas() + " bajas)")
                        .orElse("Revisala antes de abrir: cada uno vota solo lo que usa."), rev.isPresent()));
        items.add(new CalendarioDTO.Item("Semana 1", "Ciclo abierto",
                ciclo != null ? ciclo.getNombre() + " · del " + ciclo.getFechaInicio().format(DDMM) + " al "
                        + ciclo.getFechaFin().format(DDMM) : "Todavía no hay ciclo trimestral.", ciclo != null));

        if (ciclo == null) {
            return new CalendarioDTO(null, items);
        }
        boolean cerrado = ciclo.getEstado() == EstadoCiclo.CERRADO;
        List<ResultadoProcedimiento> res = cerrado ? resultadosProc.findByCicloId(ciclo.getId()) : List.of();
        long votantes;
        long habilitados;
        if (cerrado) {
            votantes = res.stream().mapToLong(ResultadoProcedimiento::getVotantes).sum();
            habilitados = res.stream().mapToLong(ResultadoProcedimiento::getHabilitados).sum();
        } else {
            votantes = votosEmitidos.countByIdCicloId(ciclo.getId());
            habilitados = matriz.contarHabilitados(TinderService.ESTADOS_VOTABLES);
        }
        int pct = habilitados == 0 ? 0 : (int) Math.round(100.0 * votantes / habilitados);
        items.add(new CalendarioDTO.Item("Semanas 2 y 3", "Participación",
                pct + " % (" + votantes + " de " + habilitados + " votos posibles). La meta es 50 % o más.", pct >= 50));

        items.add(new CalendarioDTO.Item("Fin de la semana 3", "Ciclo cerrado",
                cerrado ? "Cerrado: resultados calculados." : "Cierra el " + ciclo.getFechaFin().format(DDMM) + ".", cerrado));
        long sinCita = res.stream()
                .filter(r -> r.getDestino() == Veredicto.SEGUNDA_CITA)
                .filter(r -> !citas.existsByCicloIdAndProcedimientoId(ciclo.getId(), r.getProcedimiento().getId()))
                .count();
        long conCita = res.stream().filter(r -> r.getDestino() == Veredicto.SEGUNDA_CITA).count();
        items.add(new CalendarioDTO.Item("Fin de la semana 3", "Segundas citas convocadas",
                !cerrado ? "Se convocan solas al cerrar el ciclo."
                        : conCita == 0 ? "No hizo falta ninguna." : (conCita - sinCita) + " de " + conCita + " convocadas.",
                cerrado && sinCita == 0));

        long casamientosPend = procedimientos.countByEstado(EstadoProcedimiento.PENDIENTE_CASAMIENTO);
        long velorioPend = procedimientos.findByEstadoOrderByCodigo(EstadoProcedimiento.PENDIENTE_VELORIO).stream()
                .filter(p -> !velorios.existsByProcedimientoId(p.getId())).count();
        items.add(new CalendarioDTO.Item("Semana 4", "Ceremonias realizadas",
                casamientosPend + velorioPend == 0 ? "Nadie espera en el altar ni en la sala velatoria."
                        : "Faltan " + casamientosPend + (casamientosPend == 1 ? " casamiento" : " casamientos") + " y "
                        + velorioPend + (velorioPend == 1 ? " velorio." : " velorios."), casamientosPend + velorioPend == 0));
        long sinFirmas = velorios.contarSinFirmasCompletas();
        items.add(new CalendarioDTO.Item("Semana 4", "Actas firmadas",
                sinFirmas == 0 ? "Todas con sus tres firmas." : sinFirmas + (sinFirmas == 1 ? " acta espera" : " actas esperan") + " firmas.",
                sinFirmas == 0));
        long vencidas = mejoras.countByEstadoNotInAndFechaCompromisoBefore(MejoraService.CERRADAS, hoy);
        items.add(new CalendarioDTO.Item("Semana 4", "Tablero actualizado",
                vencidas == 0 ? "Ninguna mejora vencida." : vencidas + (vencidas == 1 ? " mejora vencida." : " mejoras vencidas."),
                vencidas == 0));
        return new CalendarioDTO(CicloDTO.de(ciclo), items);
    }
}
