package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.PerfilDTO;
import ar.gob.defensoria.tinder.web.dto.VotoResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Mazo y votación.
 * ANONIMATO: este servicio NO loguea nada. La participación (quién) y el voto (qué)
 * se guardan en tablas separadas, sin ninguna columna que las relacione.
 */
@Service
public class TinderService {

    public static final Set<EstadoProcedimiento> ESTADOS_VOTABLES =
            EnumSet.of(EstadoProcedimiento.EN_TINDER, EstadoProcedimiento.SEGUNDA_CITA);

    private final ProcedimientoRepository procedimientos;
    private final CicloVotacionRepository ciclos;
    private final ParticipacionRepository participaciones;
    private final VotoRepository votos;
    private final ResultadoCicloRepository resultados;

    public TinderService(ProcedimientoRepository procedimientos, CicloVotacionRepository ciclos,
                         ParticipacionRepository participaciones, VotoRepository votos,
                         ResultadoCicloRepository resultados) {
        this.procedimientos = procedimientos;
        this.ciclos = ciclos;
        this.participaciones = participaciones;
        this.votos = votos;
        this.resultados = resultados;
    }

    @Transactional(readOnly = true)
    public List<PerfilDTO> mazo(UsuarioAutenticado usuario) {
        return ciclos.findFirstByEstado(EstadoCiclo.ABIERTO)
                .map(ciclo -> {
                    List<Procedimiento> lista = new ArrayList<>(
                            procedimientos.findMazo(usuario.areaId(), usuario.id(), ciclo.getId(), ESTADOS_VOTABLES));
                    Collections.shuffle(lista);
                    return lista.stream().map(p -> perfil(p, usuario.areaId())).toList();
                })
                .orElse(List.of());
    }

    @Transactional
    public VotoResponse votar(UsuarioAutenticado usuario, Long procedimientoId, ValorVoto valor) {
        CicloVotacion ciclo = ciclos.findFirstByEstado(EstadoCiclo.ABIERTO)
                .orElseThrow(() -> ApiException.reglaNegocio("SIN_CICLO_ABIERTO",
                        "No hay ningún ciclo de votación abierto. Volvé cuando Calidad abra uno."));
        Procedimiento p = procedimientos.findById(procedimientoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el procedimiento " + procedimientoId));
        if (!ESTADOS_VOTABLES.contains(p.getEstado())) {
            throw ApiException.reglaNegocio("NO_VOTABLE", p.getCodigo() + " ya no está en el Tinder ("
                    + MaquinaEstados.legible(p.getEstado()) + ").");
        }
        if (p.getAreas().stream().noneMatch(a -> a.getId().equals(usuario.areaId()))) {
            throw ApiException.prohibido(p.getCodigo() + " no es de tu área: no te corresponde votarlo.");
        }

        Participacion participacion = new Participacion(usuario.id(), p.getId(), ciclo.getId());
        if (participaciones.existsById(participacion.getId())) {
            throw votoDuplicado(p, ciclo);
        }
        try {
            // Misma transacción: si falla una, no queda ninguna.
            participaciones.saveAndFlush(participacion);
            votos.save(new Voto(p.getId(), ciclo.getId(), usuario.areaId(), valor));
        } catch (DataIntegrityViolationException e) {
            throw votoDuplicado(p, ciclo);
        }
        return new VotoResponse(valor == ValorVoto.LIKE);
    }

    private static ApiException votoDuplicado(Procedimiento p, CicloVotacion ciclo) {
        return ApiException.conflicto("VOTO_DUPLICADO",
                "Ya votaste " + p.getCodigo() + " en el ciclo " + ciclo.getNombre() + ". Una sola cita por trimestre.");
    }

    private PerfilDTO perfil(Procedimiento p, Long areaId) {
        return new PerfilDTO(p.getId(), p.getCodigo(), p.getNombre(), p.getTipo(), p.getBio(),
                Mapeador.edadAnios(p.getFechaCreacion()), p.getMinutosPorUso(), p.getBeneficio(),
                usoTexto(p, areaId), Mapeador.padrino(p.getAutor()), p.isObligatorio(), p.getBaseObligatoriedad(),
                p.getAreas().stream().map(Area::getNombre).toList());
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
