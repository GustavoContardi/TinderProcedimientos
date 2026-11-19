package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.config.UsuarioAutenticado;
import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.SugerenciaVelorioDTO;
import ar.gob.defensoria.tinder.web.dto.VelorioDTO;
import ar.gob.defensoria.tinder.web.dto.VelorioRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static ar.gob.defensoria.tinder.servicio.ProcedimientoService.blancoANull;

/** Velorio y acta de defunción con trazabilidad: causa, herencia, lecciones, reencarnación y tres firmas. */
@Service
public class VelorioService {

    private final VelorioRepository velorios;
    private final ProcedimientoRepository procedimientos;
    private final ResultadoCicloRepository resultados;
    private final ResultadoProcedimientoRepository resultadosProc;
    private final ObjecionRepository objeciones;
    private final MatrizParticipacionRepository matriz;
    private final UsuarioRepository usuarios;
    private final MejoraService mejoras;
    private final MaquinaEstados maquina;

    public VelorioService(VelorioRepository velorios, ProcedimientoRepository procedimientos,
                          ResultadoCicloRepository resultados, ResultadoProcedimientoRepository resultadosProc,
                          ObjecionRepository objeciones, MatrizParticipacionRepository matriz, UsuarioRepository usuarios,
                          MejoraService mejoras, MaquinaEstados maquina) {
        this.velorios = velorios;
        this.procedimientos = procedimientos;
        this.resultados = resultados;
        this.resultadosProc = resultadosProc;
        this.objeciones = objeciones;
        this.matriz = matriz;
        this.usuarios = usuarios;
        this.mejoras = mejoras;
        this.maquina = maquina;
    }

    @Transactional(readOnly = true)
    public SugerenciaVelorioDTO sugerencia(Long procedimientoId) {
        Procedimiento p = pendienteDeVelorio(procedimientoId);
        int minutos = p.minutosSemanales();
        return new SugerenciaVelorioDTO(p.getId(), p.getCodigo(), p.getNombre(), causaSugerida(p),
                epitafioSugerido(minutos), minutos, "nadie", Mapeador.nombre(p.getDueno()),
                siguienteNumeroActa(LocalDate.now()));
    }

    public static String epitafioSugerido(int minutos) {
        return "Nos robó " + minutos + " minutos por semana";
    }

    /** "{pct}% de aprobación y {n} objeciones" más las 2 categorías de objeción más frecuentes. */
    String causaSugerida(Procedimiento p) {
        Integer pct = resultadosProc.findFirstByProcedimientoIdOrderByCicloFechaFinDescIdDesc(p.getId())
                .filter(r -> r.getVotantes() > 0)
                .map(r -> Mapeador.pct(r.getPctGlobal()))
                .orElseGet(() -> {
                    List<ResultadoCiclo> ultimos = resultados.ultimosDelProcedimiento(p.getId());
                    int likes = ultimos.stream().mapToInt(ResultadoCiclo::getLikes).sum();
                    int total = ultimos.stream().mapToInt(ResultadoCiclo::getTotal).sum();
                    return total == 0 ? null : Mapeador.pct(ClasificadorVeredicto.pct(likes, total - likes));
                });
        List<Objecion> lista = objeciones.findByProcedimientoId(p.getId());
        StringBuilder causa = new StringBuilder(pct != null ? pct + "% de aprobación" : "Sin votos que lo defiendan")
                .append(" y ").append(lista.size()).append(lista.size() == 1 ? " objeción" : " objeciones");
        List<String> top = lista.stream()
                .collect(Collectors.groupingBy(Objecion::getCategoria, () -> new EnumMap<>(CategoriaObjecion.class),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<CategoriaObjecion, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(2)
                .map(e -> ObjecionService.categoriaLegible(e.getKey()).toLowerCase() + " (" + e.getValue() + ")")
                .toList();
        if (!top.isEmpty()) {
            causa.append(". Lo que más se repitió: ").append(String.join(" y ", top));
        }
        return causa.append('.').toString();
    }

    /**
     * Crea el acta (juez de paz). Reencarnación: un procedimiento existente, uno NUEVO en BORRADOR o "sin reemplazo"
     * con motivo obligatorio. Crea mejoras ACTA_DEFUNCION por la herencia y por redactar el reemplazo.
     */
    @Transactional
    public VelorioDTO crear(VelorioRequest req, UsuarioAutenticado juez) {
        Procedimiento p = pendienteDeVelorio(req.procedimientoId());
        if (velorios.existsByProcedimientoId(p.getId())) {
            throw ApiException.conflicto("VELORIO_EXISTENTE", p.getCodigo() + " ya tiene su acta labrada.");
        }
        String herencia = blancoANull(req.herencia());
        String motivo = blancoANull(req.sinReemplazoMotivo());
        Procedimiento reencarnacion = null;
        if (req.reencarnacionId() != null) {
            reencarnacion = procedimientos.findById(req.reencarnacionId())
                    .orElseThrow(() -> ApiException.noEncontrado("No existe el procedimiento " + req.reencarnacionId()));
            if (reencarnacion.getId().equals(p.getId()) || reencarnacion.getEstado() == EstadoProcedimiento.VELADO) {
                throw ApiException.reglaNegocio("REENCARNACION_INVALIDA",
                        "La reencarnación tiene que ser otro procedimiento, vivo.");
            }
            if (reencarnacion.getReemplazaA() == null) {
                reencarnacion.setReemplazaA(p);
            }
            motivo = null;
        } else if (req.nuevoReemplazo() != null) {
            reencarnacion = nuevoBorrador(p, req.nuevoReemplazo().codigo(), req.nuevoReemplazo().nombre(), herencia);
            motivo = null;
        } else if (motivo == null) {
            throw ApiException.reglaNegocio("FALTA_REEMPLAZO",
                    "Elegí la reencarnación o, si no hay reemplazo, contá por qué.");
        }

        int minutos = p.minutosSemanales();
        LocalDate fecha = req.fecha() != null ? req.fecha() : LocalDate.now();
        String causa = blancoANull(req.causa());
        String epitafio = blancoANull(req.epitafio());
        String deudos = blancoANull(req.deudos());
        Velorio v = velorios.save(new Velorio(siguienteNumeroActa(fecha), p, fecha,
                causa != null ? causa : causaSugerida(p),
                epitafio != null ? epitafio : epitafioSugerido(minutos),
                minutos, deudos != null ? deudos : "nadie", herencia, blancoANull(req.lecciones()), reencarnacion, motivo));

        String acta = "acta " + v.getNumeroActa();
        if (herencia != null) {
            mejoras.crearAutomatica(OrigenMejora.ACTA_DEFUNCION, String.valueOf(v.getId()), p,
                    "Herencia de " + p.getCodigo() + " (" + acta + "): " + herencia, juez.id(), null);
        }
        if (reencarnacion != null && reencarnacion.getEstado() == EstadoProcedimiento.BORRADOR) {
            mejoras.crearAutomatica(OrigenMejora.ACTA_DEFUNCION, String.valueOf(v.getId()), reencarnacion,
                    "Redactar el reemplazo " + reencarnacion.getCodigo() + " de " + p.getCodigo() + " (" + acta
                            + ") y publicarlo en el Tinder.", juez.id(), null);
        }
        return Mapeador.velorio(v);
    }

    /** El reemplazo nace en BORRADOR con el dueño copiado y la misma matriz. */
    private Procedimiento nuevoBorrador(Procedimiento difunto, String codigo, String nombre, String herencia) {
        String cod = codigo.trim().toUpperCase();
        if (procedimientos.existsByCodigoIgnoreCase(cod)) {
            throw ApiException.conflicto("CODIGO_DUPLICADO", "Ya existe un procedimiento con el código " + cod);
        }
        Procedimiento r = new Procedimiento();
        r.setCodigo(cod);
        r.setNombre(nombre.trim());
        r.setTipo(difunto.getTipo());
        r.setBio("Vengo a reemplazar a " + difunto.getCodigo() + ". Todavía me están escribiendo, pero prometo ser más corto.");
        r.setFechaCreacion(LocalDate.now());
        r.setMinutosPorUso(difunto.getMinutosPorUso());
        r.setUsosPorSemana(difunto.getUsosPorSemana());
        r.setBeneficio(herencia != null ? herencia : difunto.getBeneficio());
        r.setObligatorio(false);
        r.setDueno(difunto.getDueno());
        r.setAutor(difunto.getDueno());
        r.setAreas(new LinkedHashSet<>(difunto.getAreas()));
        r.setReemplazaA(difunto);
        r.setEstado(EstadoProcedimiento.BORRADOR);
        Procedimiento guardado = procedimientos.save(r);
        LocalDate hoy = LocalDate.now();
        matriz.saveAll(matriz.findByIdProcedimientoId(difunto.getId()).stream()
                .map(m -> new MatrizParticipacion(m.getId().usuarioId(), guardado.getId(), hoy)).toList());
        return guardado;
    }

    /**
     * Registra la firma según quién firma: JUEZ_PAZ, DIRECCION o el dueño del procedimiento.
     * Una persona, una firma (409 si repite). Con las 3 firmas el procedimiento pasa a VELADO.
     */
    @Transactional
    public VelorioDTO firmar(Long id, UsuarioAutenticado firmante) {
        Velorio v = velorios.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el acta " + id));
        Procedimiento p = v.getProcedimiento();
        if (v.completa()) {
            throw ApiException.conflicto("ACTA_YA_FIRMADA", "El acta " + v.getNumeroActa()
                    + " ya tiene las tres firmas. Dejalo descansar en paz.");
        }
        if (v.firmoAlguna(firmante.id())) {
            throw ApiException.conflicto("FIRMA_REPETIDA", "Ya firmaste el acta " + v.getNumeroActa()
                    + ". Una persona, una firma.");
        }
        Usuario u = usuarios.findById(firmante.id())
                .orElseThrow(() -> ApiException.noEncontrado("No existe el usuario " + firmante.id()));
        boolean esDueno = p.getDueno() != null && p.getDueno().getId().equals(u.getId());
        if (u.getRol() == Rol.JUEZ_PAZ && v.getFirmaJuezPor() == null) {
            v.firmarComoJuez(u);
        } else if (u.getRol() == Rol.DIRECCION && v.getFirmaDireccionPor() == null) {
            v.firmarComoDireccion(u);
        } else if (esDueno && v.getFirmaDuenoPor() == null) {
            v.firmarComoDueno(u);
        } else if (u.getRol() == Rol.JUEZ_PAZ || u.getRol() == Rol.DIRECCION || esDueno) {
            throw ApiException.conflicto("FIRMA_REPETIDA", "Esa firma del acta " + v.getNumeroActa() + " ya está.");
        } else {
            throw ApiException.prohibido("El acta la firman el juez de paz, la Dirección y el dueño de " + p.getCodigo() + ".");
        }
        if (v.completa()) {
            maquina.transicionar(p, EstadoProcedimiento.VELADO);
        }
        return Mapeador.velorio(v);
    }

    @Transactional(readOnly = true)
    public List<VelorioDTO> listar() {
        return velorios.findAllByOrderByFechaDescIdDesc().stream().map(Mapeador::velorio).toList();
    }

    @Transactional(readOnly = true)
    public VelorioDTO obtener(Long id) {
        return Mapeador.velorio(velorios.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el acta " + id)));
    }

    private String siguienteNumeroActa(LocalDate fecha) {
        String anio = String.valueOf(fecha.getYear());
        return "%03d/%s".formatted(velorios.ultimoNumeroDelAnio(anio) + 1, anio);
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
