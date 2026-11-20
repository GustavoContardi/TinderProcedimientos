package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.ObjecionRequest;
import ar.gob.defensoria.tinder.web.dto.PerfilDTO;
import ar.gob.defensoria.tinder.web.dto.VotoRequest;
import ar.gob.defensoria.tinder.web.dto.VotoResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Mazo y votación.
 * ANONIMATO: este servicio NO loguea nada. Quién votó (voto_emitido), qué votó (voto) y por qué objetó (objecion)
 * se guardan en tablas separadas, en la misma transacción y sin ninguna columna que las relacione.
 */
@Service
public class TinderService {

    public static final Set<EstadoProcedimiento> ESTADOS_VOTABLES =
            EnumSet.of(EstadoProcedimiento.EN_TINDER, EstadoProcedimiento.SEGUNDA_CITA);
    public static final String FUERA_DE_MATRIZ = "Solo podés votar los procedimientos en los que participás";

    private final ProcedimientoRepository procedimientos;
    private final CicloVotacionRepository ciclos;
    private final VotoEmitidoRepository votosEmitidos;
    private final VotoRepository votos;
    private final ObjecionRepository objeciones;
    private final MatrizParticipacionRepository matriz;
    private final ResultadoCicloRepository resultados;

    public TinderService(ProcedimientoRepository procedimientos, CicloVotacionRepository ciclos,
                         VotoEmitidoRepository votosEmitidos, VotoRepository votos, ObjecionRepository objeciones,
                         MatrizParticipacionRepository matriz, ResultadoCicloRepository resultados) {
        this.procedimientos = procedimientos;
        this.ciclos = ciclos;
        this.votosEmitidos = votosEmitidos;
        this.votos = votos;
        this.objeciones = objeciones;
        this.matriz = matriz;
        this.resultados = resultados;
    }

    /**
     * Procedimientos de la matriz del usuario que todavía no votó: los del ciclo trimestral abierto
     * (EN_TINDER o SEGUNDA_CITA) más los que tienen una votación rápida abierta.
     */
    @Transactional(readOnly = true)
    public List<PerfilDTO> mazo(UsuarioAutenticado usuario) {
        List<PerfilDTO> mazo = new ArrayList<>();
        ciclos.findFirstByEstadoAndTipo(EstadoCiclo.ABIERTO, TipoCiclo.TRIMESTRAL).ifPresent(ciclo ->
                procedimientos.findMazo(usuario.id(), ciclo.getId(), ESTADOS_VOTABLES)
                        .forEach(p -> mazo.add(perfil(p, ciclo, usuario.areaId()))));
        for (CicloVotacion rapida : ciclos.findByEstadoAndTipo(EstadoCiclo.ABIERTO, TipoCiclo.RAPIDA)) {
            Procedimiento p = rapida.getProcedimiento();
            if (matriz.existsById(new MatrizParticipacion.Id(usuario.id(), p.getId()))
                    && !votosEmitidos.existsById(new VotoEmitido.Id(usuario.id(), p.getId(), rapida.getId()))) {
                mazo.add(perfil(p, rapida, usuario.areaId()));
            }
        }
        Collections.shuffle(mazo);
        return mazo;
    }

    @Transactional
    public VotoResponse votar(UsuarioAutenticado usuario, VotoRequest req) {
        ObjecionRequest objecion = req.objecion();
        if (objecion != null && req.valor() == ValorVoto.LIKE) {
            throw ApiException.pedidoInvalido("OBJECION_CON_LIKE",
                    "Solo se objeta un NOPE: si te sirve, no hay nada que objetar.");
        }
        if (req.valor() == ValorVoto.NOPE) {
            if (objecion == null || objecion.categoria() == null) {
                throw ApiException.pedidoInvalido("MOTIVO_OBLIGATORIO",
                        "Para rechazar un procedimiento hay que indicar el motivo (categoría de la objeción).");
            }
            if (objecion.categoria() == CategoriaObjecion.OTRO && ProcedimientoService.blancoANull(objecion.texto()) == null) {
                throw ApiException.pedidoInvalido("MOTIVO_OBLIGATORIO",
                        "Con \"Otro motivo\" hay que describirlo.");
            }
        }
        CicloVotacion ciclo = ciclos.findById(req.cicloId())
                .orElseThrow(() -> ApiException.noEncontrado("No existe el ciclo " + req.cicloId()));
        if (ciclo.getEstado() != EstadoCiclo.ABIERTO) {
            throw ApiException.reglaNegocio("CICLO_CERRADO", "El ciclo " + ciclo.getNombre() + " ya cerró.");
        }
        Procedimiento p = procedimientos.findById(req.procedimientoId())
                .orElseThrow(() -> ApiException.noEncontrado("No existe el procedimiento " + req.procedimientoId()));
        if (!matriz.existsById(new MatrizParticipacion.Id(usuario.id(), p.getId()))) {
            throw ApiException.prohibido(FUERA_DE_MATRIZ);
        }
        validarQueSeVotaEnEsteCiclo(p, ciclo);

        VotoEmitido emitido = new VotoEmitido(usuario.id(), p.getId(), ciclo.getId());
        if (votosEmitidos.existsById(emitido.getId())) {
            throw votoDuplicado(p, ciclo);
        }
        try {
            // Misma transacción: si falla una, no queda ninguna. Ninguna guarda quién votó qué ni cuándo.
            votosEmitidos.saveAndFlush(emitido);
            votos.save(new Voto(p.getId(), ciclo.getId(), usuario.areaId(), req.valor()));
            if (objecion != null) {
                objeciones.save(new Objecion(p.getId(), ciclo.getId(), usuario.areaId(), OrigenObjecion.VOTO,
                        objecion.categoria(), ProcedimientoService.blancoANull(objecion.texto())));
            }
        } catch (DataIntegrityViolationException e) {
            throw votoDuplicado(p, ciclo);
        }
        return new VotoResponse(req.valor() == ValorVoto.LIKE);
    }

    private void validarQueSeVotaEnEsteCiclo(Procedimiento p, CicloVotacion ciclo) {
        if (ciclo.esRapida()) {
            if (!ciclo.getProcedimiento().getId().equals(p.getId())) {
                throw ApiException.reglaNegocio("NO_VOTABLE", "La votación rápida " + ciclo.getNombre()
                        + " es solo de " + ciclo.getProcedimiento().getCodigo() + ".");
            }
            return;
        }
        if (!ESTADOS_VOTABLES.contains(p.getEstado())) {
            throw ApiException.reglaNegocio("NO_VOTABLE", p.getCodigo() + " ya no está en el Tinder ("
                    + MaquinaEstados.legible(p.getEstado()) + ").");
        }
        if (ciclos.existsByProcedimientoIdAndEstadoAndTipo(p.getId(), EstadoCiclo.ABIERTO, TipoCiclo.RAPIDA)) {
            throw ApiException.reglaNegocio("NO_VOTABLE", p.getCodigo() + " tiene una votación rápida abierta: votalo ahí.");
        }
    }

    private static ApiException votoDuplicado(Procedimiento p, CicloVotacion ciclo) {
        return ApiException.conflicto("VOTO_DUPLICADO",
                "Ya votaste " + p.getCodigo() + " en el ciclo " + ciclo.getNombre() + ". Una sola cita por ciclo.");
    }

    private PerfilDTO perfil(Procedimiento p, CicloVotacion ciclo, Long areaId) {
        return new PerfilDTO(p.getId(), p.getCodigo(), p.getNombre(), p.getTipo(), p.getBio(),
                Mapeador.edadAnios(p.getFechaCreacion()), p.getMinutosPorUso(), p.getBeneficio(),
                usoTexto(p, areaId), Mapeador.nombre(p.getAutor()), Mapeador.nombre(p.getDueno()), p.getNumeroVersion(),
                p.isObligatorio(), p.getBaseObligatoriedad(), p.getAreas().stream().map(Area::getNombre).toList(),
                ciclo.getId(), ciclo.getNombre(), ciclo.esRapida());
    }

    /** "Me usan X de cada 10" según el último ciclo con quórum en el área; si no hay datos, la frecuencia de uso. */
    private String usoTexto(Procedimiento p, Long areaId) {
        return resultados.findFirstByProcedimientoIdAndAreaIdOrderByCicloFechaFinDesc(p.getId(), areaId)
                .filter(r -> r.getVeredicto() != Veredicto.QUORUM_INSUFICIENTE)
                .map(r -> "Me usan " + r.getPctLike().multiply(BigDecimal.TEN).setScale(0, RoundingMode.HALF_UP)
                        + " de cada 10")
                .orElse("Me usan " + p.getUsosPorSemana() + (p.getUsosPorSemana() == 1 ? " vez" : " veces") + " por semana");
    }
}
