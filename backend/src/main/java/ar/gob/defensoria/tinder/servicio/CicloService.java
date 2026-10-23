package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.CicloDTO;
import ar.gob.defensoria.tinder.web.dto.CicloRequest;
import ar.gob.defensoria.tinder.web.dto.ResumenCierreDTO;
import ar.gob.defensoria.tinder.web.dto.TransicionDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CicloService {

    private final CicloVotacionRepository ciclos;
    private final ProcedimientoRepository procedimientos;
    private final VotoRepository votos;
    private final ResultadoCicloRepository resultados;
    private final AlertaRepository alertas;
    private final ClasificadorVeredicto clasificador;
    private final MaquinaEstados maquina;

    public CicloService(CicloVotacionRepository ciclos, ProcedimientoRepository procedimientos, VotoRepository votos,
                        ResultadoCicloRepository resultados, AlertaRepository alertas,
                        ClasificadorVeredicto clasificador, MaquinaEstados maquina) {
        this.ciclos = ciclos;
        this.procedimientos = procedimientos;
        this.votos = votos;
        this.resultados = resultados;
        this.alertas = alertas;
        this.clasificador = clasificador;
        this.maquina = maquina;
    }

    @Transactional(readOnly = true)
    public Optional<CicloDTO> actual() {
        return ciclos.findFirstByEstado(EstadoCiclo.ABIERTO).map(CicloDTO::de);
    }

    @Transactional(readOnly = true)
    public List<CicloDTO> listar() {
        return ciclos.findAllByOrderByFechaInicioDesc().stream().map(CicloDTO::de).toList();
    }

    @Transactional
    public CicloDTO abrir(CicloRequest req) {
        ciclos.findFirstByEstado(EstadoCiclo.ABIERTO).ifPresent(c -> {
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
     * Cierra el ciclo: calcula resultados por procedimiento y área, consolida y aplica las transiciones de estado.
     */
    @Transactional
    public ResumenCierreDTO cerrar(Long cicloId) {
        CicloVotacion ciclo = ciclos.findById(cicloId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el ciclo " + cicloId));
        if (ciclo.getEstado() == EstadoCiclo.CERRADO) {
            throw ApiException.reglaNegocio("CICLO_CERRADO", "El ciclo " + ciclo.getNombre() + " ya estaba cerrado.");
        }

        // (procedimientoId, areaId) -> [likes, nopes]
        Map<String, int[]> conteos = new HashMap<>();
        for (ConteoVotos c : votos.contarPorCiclo(ciclo.getId())) {
            int[] par = conteos.computeIfAbsent(c.procedimientoId() + ":" + c.areaId(), k -> new int[2]);
            par[c.valor() == ValorVoto.LIKE ? 0 : 1] += (int) c.cantidad();
        }

        List<Procedimiento> enJuego = procedimientos.findByEstadoIn(TinderService.ESTADOS_VOTABLES).stream()
                .sorted(Comparator.comparing(Procedimiento::getCodigo)).toList();

        int calculados = 0;
        List<TransicionDTO> transiciones = new ArrayList<>();
        for (Procedimiento p : enJuego) {
            Map<String, Veredicto> porArea = new LinkedHashMap<>();
            for (Area area : p.getAreas()) {
                int[] par = conteos.getOrDefault(p.getId() + ":" + area.getId(), new int[2]);
                Veredicto v = clasificador.clasificar(par[0], par[1]);
                resultados.save(new ResultadoCiclo(ciclo, p, area, par[0], par[1],
                        ClasificadorVeredicto.pctLike(par[0], par[1]), v));
                porArea.put(area.getNombre(), v);
                calculados++;
            }
            DecisionCierre decision = maquina.resolverCierre(p, porArea);
            EstadoProcedimiento desde = p.getEstado();
            maquina.transicionar(p, decision.destino());
            if (decision.alerta() != null) {
                alertas.save(new Alerta(p, ciclo, decision.alerta(), p.getCodigo() + ": " + decision.mensaje()));
            }
            transiciones.add(new TransicionDTO(p.getId(), p.getCodigo(), p.getNombre(), desde, p.getEstado(),
                    decision.mensaje()));
        }
        ciclo.cerrar();
        return new ResumenCierreDTO(CicloDTO.de(ciclo), calculados, transiciones);
    }
}
