package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.TinderProperties;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.CicloDTO;
import ar.gob.defensoria.tinder.web.dto.CicloRequest;
import ar.gob.defensoria.tinder.web.dto.ResumenCierreDTO;
import ar.gob.defensoria.tinder.web.dto.TransicionDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ar.gob.defensoria.tinder.dominio.EstadoProcedimiento.*;

@Service
public class CicloService {

    private final CicloVotacionRepository ciclos;
    private final ProcedimientoRepository procedimientos;
    private final VotoRepository votos;
    private final ResultadoCicloRepository resultados;
    private final ResultadoProcedimientoRepository resultadosProc;
    private final MatrizParticipacionRepository matriz;
    private final AreaRepository areas;
    private final AlertaRepository alertas;
    private final SegundaCitaRepository citas;
    private final MejoraService mejoras;
    private final ClasificadorVeredicto clasificador;
    private final MaquinaEstados maquina;
    private final TinderProperties props;

    public CicloService(CicloVotacionRepository ciclos, ProcedimientoRepository procedimientos, VotoRepository votos,
                        ResultadoCicloRepository resultados, ResultadoProcedimientoRepository resultadosProc,
                        MatrizParticipacionRepository matriz, AreaRepository areas, AlertaRepository alertas,
                        SegundaCitaRepository citas, MejoraService mejoras, ClasificadorVeredicto clasificador,
                        MaquinaEstados maquina, TinderProperties props) {
        this.ciclos = ciclos;
        this.procedimientos = procedimientos;
        this.votos = votos;
        this.resultados = resultados;
        this.resultadosProc = resultadosProc;
        this.matriz = matriz;
        this.areas = areas;
        this.alertas = alertas;
        this.citas = citas;
        this.mejoras = mejoras;
        this.clasificador = clasificador;
        this.maquina = maquina;
        this.props = props;
    }

    /** Ciclo trimestral abierto. */
    @Transactional(readOnly = true)
    public Optional<CicloDTO> actual() {
        return ciclos.findFirstByEstadoAndTipo(EstadoCiclo.ABIERTO, TipoCiclo.TRIMESTRAL).map(CicloDTO::de);
    }

    @Transactional(readOnly = true)
    public List<CicloDTO> listar() {
        return ciclos.findAllByOrderByFechaInicioDescIdDesc().stream().map(CicloDTO::de).toList();
    }

    @Transactional
    public CicloDTO abrir(CicloRequest req) {
        ciclos.findFirstByEstadoAndTipo(EstadoCiclo.ABIERTO, TipoCiclo.TRIMESTRAL).ifPresent(c -> {
            throw ApiException.conflicto("CICLO_YA_ABIERTO",
                    "Ya hay un ciclo abierto (" + c.getNombre() + "). Cerralo antes de abrir otro.");
        });
        if (ciclos.existsByNombreIgnoreCase(req.nombre().trim())) {
            throw ApiException.conflicto("CICLO_DUPLICADO", "Ya existe un ciclo llamado " + req.nombre());
        }
        if (req.fechaFin().isBefore(req.fechaInicio())) {
            throw ApiException.reglaNegocio("FECHAS_INVALIDAS", "La fecha de fin no puede ser anterior a la de inicio.");
        }
        return CicloDTO.de(ciclos.save(new CicloVotacion(req.nombre().trim(), req.fechaInicio(), req.fechaFin())));
    }

    /**
     * Cierra el ciclo (trimestral o rápido): calcula resultados por área y por procedimiento y aplica los destinos.
     *
     * @param actorId juez de paz que cierra (responsable de respaldo para las mejoras automáticas)
     */
    @Transactional
    public ResumenCierreDTO cerrar(Long cicloId, Long actorId) {
        CicloVotacion ciclo = ciclos.findById(cicloId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el ciclo " + cicloId));
        if (ciclo.getEstado() == EstadoCiclo.CERRADO) {
            throw ApiException.reglaNegocio("CICLO_CERRADO", "El ciclo " + ciclo.getNombre() + " ya estaba cerrado.");
        }

        // procedimientoId -> areaId -> [likes, nopes]
        Map<Long, Map<Long, int[]>> conteos = new HashMap<>();
        for (ConteoVotos c : votos.contarPorCiclo(ciclo.getId())) {
            int[] par = conteos.computeIfAbsent(c.procedimientoId(), k -> new HashMap<>())
                    .computeIfAbsent(c.areaId(), k -> new int[2]);
            par[c.valor() == ValorVoto.LIKE ? 0 : 1] += (int) c.cantidad();
        }
        Map<Long, Area> porId = areas.findAll().stream().collect(Collectors.toMap(Area::getId, Function.identity()));

        List<Procedimiento> enJuego = ciclo.esRapida() ? List.of(ciclo.getProcedimiento()) : enJuegoTrimestral();
        int calculados = 0;
        List<TransicionDTO> transiciones = new ArrayList<>();
        for (Procedimiento p : enJuego) {
            Map<Long, int[]> delProc = conteos.getOrDefault(p.getId(), Map.of());
            Set<Long> areaIds = new TreeSet<>(matriz.areasDelProcedimiento(p.getId()));
            areaIds.addAll(delProc.keySet());
            List<EvaluacionProcedimiento.Conteo> conteoAreas = areaIds.stream()
                    .filter(porId::containsKey)
                    .map(id -> {
                        int[] par = delProc.getOrDefault(id, new int[2]);
                        return new EvaluacionProcedimiento.Conteo(id, porId.get(id).getNombre(), par[0], par[1]);
                    })
                    .sorted(Comparator.comparing(EvaluacionProcedimiento.Conteo::areaNombre))
                    .toList();
            int habilitados = (int) matriz.countByIdProcedimientoId(p.getId());
            EvaluacionProcedimiento ev = clasificador.evaluar(p.isObligatorio(), p.getBaseObligatoriedad(), habilitados,
                    conteoAreas);

            for (EvaluacionProcedimiento.Area a : ev.areas()) {
                var c = a.conteo();
                resultados.save(new ResultadoCiclo(ciclo, p, porId.get(c.areaId()), c.likes(), c.nopes(), a.pct(),
                        a.veredicto()));
                calculados++;
            }

            EstadoProcedimiento desde = p.getEstado();
            Destino destino = ciclo.esRapida() ? aplicarRapida(p, ciclo, ev, actorId) : aplicar(p, ciclo, ev, actorId);
            resultadosProc.save(new ResultadoProcedimiento(ciclo, p, ev.pctGlobal(), ev.votantes(), ev.habilitados(),
                    destino.veredicto(), ev.relacionADistancia(), ev.grupal(), destino.mensaje()));
            alertar(p, ciclo, ev, destino);
            transiciones.add(new TransicionDTO(p.getId(), p.getCodigo(), p.getNombre(), destino.veredicto(), desde,
                    p.getEstado(), destino.mensaje()));
        }
        ciclo.cerrar();
        return new ResumenCierreDTO(CicloDTO.de(ciclo), calculados, transiciones);
    }

    /** EN_TINDER y SEGUNDA_CITA, salvo los que tienen una votación rápida abierta (se cierran aparte). */
    private List<Procedimiento> enJuegoTrimestral() {
        Set<Long> conRapida = ciclos.findByEstadoAndTipo(EstadoCiclo.ABIERTO, TipoCiclo.RAPIDA).stream()
                .map(c -> c.getProcedimiento().getId()).collect(Collectors.toSet());
        return procedimientos.findByEstadoIn(TinderService.ESTADOS_VOTABLES).stream()
                .filter(p -> !conRapida.contains(p.getId()))
                .sorted(Comparator.comparing(Procedimiento::getCodigo))
                .toList();
    }

    private record Destino(Veredicto veredicto, String mensaje) {
    }

    /** Transiciones del cierre trimestral (y de la votación rápida de AJUSTAR_Y_REVOTAR). */
    private Destino aplicar(Procedimiento p, CicloVotacion ciclo, EvaluacionProcedimiento ev, Long actorId) {
        switch (ev.destino()) {
            case CASAMIENTO -> {
                maquina.transicionar(p, PENDIENTE_CASAMIENTO);
                p.setSegundasCitasConsecutivas(0);
            }
            case VELORIO -> {
                maquina.transicionar(p, PENDIENTE_VELORIO);
                p.setSegundasCitasConsecutivas(0);
            }
            case SEGUNDA_CITA -> {
                maquina.transicionar(p, SEGUNDA_CITA);
                citas.save(new SegundaCita(p, ciclo, LocalDate.now().plusDays(props.diasHastaSegundaCita())));
                p.setSegundasCitasConsecutivas(p.getSegundasCitasConsecutivas() + 1);
                if (p.getSegundasCitasConsecutivas() >= 2 && !p.isRequiereReformulacion()) {
                    p.setRequiereReformulacion(true);
                    mejoras.crearAutomatica(OrigenMejora.REFORMULACION, String.valueOf(p.getId()), p,
                            "Reformular " + p.getCodigo() + ": " + p.getSegundasCitasConsecutivas()
                                    + " segundas citas seguidas sin un resultado claro.", actorId, null);
                    return new Destino(Veredicto.SEGUNDA_CITA, ev.mensaje() + " Ya van "
                            + p.getSegundasCitasConsecutivas() + " segundas citas seguidas: requiere reformulación.");
                }
            }
            case QUORUM_INSUFICIENTE, PARTICIPACION_INSUFICIENTE -> p.setSegundasCitasConsecutivas(0);
        }
        return new Destino(ev.destino(), ev.mensaje());
    }

    /**
     * Cierre de una votación rápida según la decisión de la segunda cita.
     * CASAR_CON_CONDICIONES: con 70 % o más → PENDIENTE_CASAMIENTO; si no, sigue en SEGUNDA_CITA.
     * VELORIO: con menos de 40 % → PENDIENTE_VELORIO; si no, sigue en SEGUNDA_CITA.
     * AJUSTAR_Y_REVOTAR: se aplica el algoritmo completo, igual que en el trimestre.
     */
    private Destino aplicarRapida(Procedimiento p, CicloVotacion ciclo, EvaluacionProcedimiento ev, Long actorId) {
        DecisionSegundaCita decision = citas.findFirstByCicloRapidoId(ciclo.getId())
                .map(SegundaCita::getDecision).orElse(DecisionSegundaCita.AJUSTAR_Y_REVOTAR);
        if (decision == DecisionSegundaCita.AJUSTAR_Y_REVOTAR) {
            return aplicar(p, ciclo, ev, actorId);
        }
        if (!ev.cambiaEstado()) {
            return new Destino(ev.destino(), ev.mensaje() + " Sigue en segunda cita.");
        }
        int pct = Mapeador.pct(ev.pctGlobal());
        if (decision == DecisionSegundaCita.CASAR_CON_CONDICIONES) {
            if (clasificador.alcanzaCasamiento(ev.pctGlobal())) {
                maquina.transicionar(p, PENDIENTE_CASAMIENTO);
                p.setSegundasCitasConsecutivas(0);
                return new Destino(Veredicto.CASAMIENTO, pct + " % en la votación rápida: se casa con condiciones.");
            }
            return new Destino(Veredicto.SEGUNDA_CITA, pct + " % en la votación rápida: no llegó al 70 %, vuelve a segunda cita.");
        }
        if (clasificador.caeEnVelorio(ev.pctGlobal())) {
            maquina.transicionar(p, PENDIENTE_VELORIO);
            p.setSegundasCitasConsecutivas(0);
            return new Destino(Veredicto.VELORIO, pct + " % en la votación rápida: se confirma el velorio.");
        }
        return new Destino(Veredicto.SEGUNDA_CITA, pct + " % en la votación rápida: no se vela, vuelve a segunda cita.");
    }

    private void alertar(Procedimiento p, CicloVotacion ciclo, EvaluacionProcedimiento ev, Destino destino) {
        TipoAlerta tipo = null;
        if (ev.relacionADistancia()) {
            tipo = TipoAlerta.RELACION_A_DISTANCIA;
        } else if (p.isObligatorio() && destino.mensaje().startsWith(ClasificadorVeredicto.MENSAJE_OBLIGATORIO)) {
            tipo = TipoAlerta.OBLIGATORIO_NO_VELABLE;
        } else if (ev.grupal() && destino.veredicto() == Veredicto.CASAMIENTO) {
            tipo = TipoAlerta.CANDIDATO_CASAMIENTO_GRUPAL;
        }
        if (tipo != null) {
            alertas.save(new Alerta(p, ciclo, tipo, p.getCodigo() + ": " + destino.mensaje()));
        }
    }
}
