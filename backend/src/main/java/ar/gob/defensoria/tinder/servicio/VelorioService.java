package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.ProcedimientoRepository;
import ar.gob.defensoria.tinder.repositorio.ResultadoCicloRepository;
import ar.gob.defensoria.tinder.repositorio.UsuarioRepository;
import ar.gob.defensoria.tinder.repositorio.VelorioRepository;
import ar.gob.defensoria.tinder.web.dto.SugerenciaVelorioDTO;
import ar.gob.defensoria.tinder.web.dto.VelorioDTO;
import ar.gob.defensoria.tinder.web.dto.VelorioRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static ar.gob.defensoria.tinder.servicio.ProcedimientoService.blancoANull;

@Service
public class VelorioService {

    private final VelorioRepository velorios;
    private final ProcedimientoRepository procedimientos;
    private final ResultadoCicloRepository resultados;
    private final UsuarioRepository usuarios;
    private final MaquinaEstados maquina;

    public VelorioService(VelorioRepository velorios, ProcedimientoRepository procedimientos,
                          ResultadoCicloRepository resultados, UsuarioRepository usuarios, MaquinaEstados maquina) {
        this.velorios = velorios;
        this.procedimientos = procedimientos;
        this.resultados = resultados;
        this.usuarios = usuarios;
        this.maquina = maquina;
    }

    @Transactional(readOnly = true)
    public SugerenciaVelorioDTO sugerencia(Long procedimientoId) {
        Procedimiento p = pendienteDeVelorio(procedimientoId);
        int minutos = p.minutosSemanales();
        return new SugerenciaVelorioDTO(p.getId(), p.getCodigo(), p.getNombre(), causaSugerida(p),
                epitafioSugerido(p.getCodigo(), p.getNombre(), minutos), minutos, "nadie");
    }

    public static String epitafioSugerido(String codigo, String nombre, int minutos) {
        return "Aquí yace " + codigo + " " + nombre + ". Nos robó " + minutos
                + " minutos por semana. No lo vamos a extrañar.";
    }

    private String causaSugerida(Procedimiento p) {
        List<ResultadoCiclo> ultimos = resultados.ultimosDelProcedimiento(p.getId());
        if (ultimos.isEmpty()) {
            return "Divorcio de común acuerdo. Nadie lo usaba.";
        }
        int nopes = ultimos.stream().mapToInt(ResultadoCiclo::getNopes).sum();
        return nopes + " swipes a la izquierda en el ciclo " + ultimos.get(0).getCiclo().getNombre();
    }

    @Transactional
    public VelorioDTO crear(VelorioRequest req) {
        Procedimiento p = pendienteDeVelorio(req.procedimientoId());
        if (velorios.existsByProcedimientoId(p.getId())) {
            throw ApiException.conflicto("VELORIO_EXISTENTE", p.getCodigo() + " ya tiene su velorio organizado.");
        }
        int minutos = p.minutosSemanales();
        String causa = blancoANull(req.causaMuerte());
        String epitafio = blancoANull(req.epitafio());
        String deudos = blancoANull(req.deudos());
        Velorio v = velorios.save(new Velorio(p,
                req.fecha() != null ? req.fecha() : LocalDate.now(),
                causa != null ? causa : causaSugerida(p),
                epitafio != null ? epitafio : epitafioSugerido(p.getCodigo(), p.getNombre(), minutos),
                minutos,
                deudos != null ? deudos : "nadie"));
        return Mapeador.velorio(v);
    }

    /** Solo DIRECCION: firma el acta y el procedimiento queda VELADO (obsoleto, nunca borrado). */
    @Transactional
    public VelorioDTO firmar(Long id, Long directorId) {
        Velorio v = velorios.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el velorio " + id));
        if (v.isActaFirmada()) {
            throw ApiException.reglaNegocio("ACTA_YA_FIRMADA", "El acta de " + v.getProcedimiento().getCodigo()
                    + " ya estaba firmada. Dejalo descansar en paz.");
        }
        Usuario director = usuarios.findById(directorId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el usuario " + directorId));
        if (director.getRol() != Rol.DIRECCION) {
            throw ApiException.prohibido("Solo la Dirección firma actas de defunción.");
        }
        maquina.transicionar(v.getProcedimiento(), EstadoProcedimiento.VELADO);
        v.firmar(director);
        return Mapeador.velorio(v);
    }

    @Transactional(readOnly = true)
    public List<VelorioDTO> listar() {
        return velorios.findAllByOrderByFechaDescIdDesc().stream().map(Mapeador::velorio).toList();
    }

    @Transactional(readOnly = true)
    public VelorioDTO obtener(Long id) {
        return Mapeador.velorio(velorios.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el velorio " + id)));
    }

    private Procedimiento pendienteDeVelorio(Long procedimientoId) {
        Procedimiento p = procedimientos.findById(procedimientoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el procedimiento " + procedimientoId));
        if (p.getEstado() != EstadoProcedimiento.PENDIENTE_VELORIO) {
            throw ApiException.reglaNegocio("TRANSICION_INVALIDA", p.getCodigo() + " no está pendiente de velorio ("
                    + MaquinaEstados.legible(p.getEstado()) + "). Todavía respira.");
        }
        return p;
    }
}
