package ar.gob.defensoria.tinder.servicio;

import ar.gob.defensoria.tinder.dominio.*;
import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.repositorio.*;
import ar.gob.defensoria.tinder.web.dto.NuevoReemplazoRequest;
import ar.gob.defensoria.tinder.web.dto.VelorioRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static ar.gob.defensoria.tinder.servicio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VelorioServiceTest {

    @Mock VelorioRepository velorios;
    @Mock ProcedimientoRepository procedimientos;
    @Mock ResultadoCicloRepository resultados;
    @Mock ResultadoProcedimientoRepository resultadosProc;
    @Mock ObjecionRepository objeciones;
    @Mock MatrizParticipacionRepository matriz;
    @Mock UsuarioRepository usuarios;
    @Mock MejoraRepository mejoraRepo;

    private VelorioService service;
    private Procedimiento f23;
    private final Usuario juez = usuario(13L, "Mónica Calderón", Rol.JUEZ_PAZ, OPERACIONES);
    private final Usuario direccion = usuario(14L, "Ricardo Ferreyra", Rol.DIRECCION, DESARROLLO);
    private final Usuario bruno = usuario(2L, "Bruno Lescano", Rol.EMPLEADO, OPERACIONES);
    private final Usuario ana = usuario(1L, "Ana Paredes", Rol.EMPLEADO, OPERACIONES);

    @BeforeEach
    void setUp() {
        service = new VelorioService(velorios, procedimientos, resultados, resultadosProc, objeciones, matriz, usuarios,
                new MejoraService(mejoraRepo, procedimientos, usuarios, props()), new MaquinaEstados());
        f23 = procedimiento(10L, "F-23", false, EstadoProcedimiento.PENDIENTE_VELORIO, OPERACIONES, DESARROLLO);
        f23.setDueno(bruno);
        lenient().when(usuarios.findById(13L)).thenReturn(Optional.of(juez));
        lenient().when(usuarios.findById(14L)).thenReturn(Optional.of(direccion));
        lenient().when(usuarios.findById(2L)).thenReturn(Optional.of(bruno));
        lenient().when(usuarios.findById(1L)).thenReturn(Optional.of(ana));
        lenient().when(mejoraRepo.siguienteNumero()).thenReturn(9L, 10L);
        lenient().when(mejoraRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Velorio acta() {
        Velorio v = new Velorio("004/2026", f23, LocalDate.now(), "causa", "epitafio", 40, "ninguno", null, null, null,
                "Lo reemplaza el sistema de tickets");
        v.setId(4L);
        when(velorios.findById(4L)).thenReturn(Optional.of(v));
        return v;
    }

    @Test
    @DisplayName("El procedimiento pasa a VELADO recién con las 3 firmas (juez, Dirección y dueño)")
    void veladoConTresFirmas() {
        Velorio v = acta();

        service.firmar(4L, autenticado(juez));
        assertThat(f23.getEstado()).isEqualTo(EstadoProcedimiento.PENDIENTE_VELORIO);
        service.firmar(4L, autenticado(direccion));
        assertThat(f23.getEstado()).isEqualTo(EstadoProcedimiento.PENDIENTE_VELORIO);
        var dto = service.firmar(4L, autenticado(bruno));

        assertThat(f23.getEstado()).isEqualTo(EstadoProcedimiento.VELADO);
        assertThat(dto.completa()).isTrue();
        assertThat(v.getFirmaJuezPor()).isEqualTo(juez);
        assertThat(v.getFirmaDireccionPor()).isEqualTo(direccion);
        assertThat(v.getFirmaDuenoPor()).isEqualTo(bruno);
    }

    @Test
    @DisplayName("Firmar dos veces => 409")
    void firmaRepetida() {
        acta();
        service.firmar(4L, autenticado(juez));

        assertThatThrownBy(() -> service.firmar(4L, autenticado(juez)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Alguien que no es juez, Dirección ni dueño no firma => 403")
    void noFirmaCualquiera() {
        acta();
        assertThatThrownBy(() -> service.firmar(4L, autenticado(ana)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("Velorio con reencarnación BORRADOR => crea la mejora \"redactar el reemplazo\" (y la de la herencia)")
    void reencarnacionBorradorCreaMejora() {
        when(procedimientos.findById(10L)).thenReturn(Optional.of(f23));
        when(procedimientos.existsByCodigoIgnoreCase("PR-11")).thenReturn(false);
        when(procedimientos.save(any())).thenAnswer(i -> {
            Procedimiento p = i.getArgument(0);
            p.setId(11L);
            return p;
        });
        when(matriz.findByIdProcedimientoId(10L)).thenReturn(List.of(new MatrizParticipacion(2L, 10L, LocalDate.now())));
        when(velorios.ultimoNumeroDelAnio(any())).thenReturn(3);
        when(velorios.save(any())).thenAnswer(i -> {
            Velorio v = i.getArgument(0);
            v.setId(4L);
            return v;
        });

        var dto = service.crear(new VelorioRequest(10L, null, null, null, null, "Los datos pasan a tickets", "Nunca más",
                null, new NuevoReemplazoRequest("pr-11", "Parte semanal en tickets"), null), autenticado(juez));

        assertThat(dto.numeroActa()).isEqualTo("004/" + LocalDate.now().getYear());
        assertThat(dto.reencarnacionCodigo()).isEqualTo("PR-11");
        assertThat(dto.reencarnacionEstado()).isEqualTo(EstadoProcedimiento.BORRADOR);
        assertThat(dto.epitafio()).isEqualTo("Insumía 40 minutos por semana");
        ArgumentCaptor<Mejora> m = ArgumentCaptor.forClass(Mejora.class);
        verify(mejoraRepo, times(2)).save(m.capture());
        assertThat(m.getAllValues()).allMatch(x -> x.getOrigen() == OrigenMejora.ACTA_DEFUNCION);
        assertThat(m.getAllValues().get(1).getDescripcion()).startsWith("Redactar el reemplazo PR-11");
        assertThat(m.getAllValues().get(1).getResponsable()).isEqualTo(bruno);
        verify(matriz).saveAll(argThat(l -> l.iterator().next().getId().procedimientoId().equals(11L)));
    }

    @Test
    @DisplayName("Sin reemplazo hay que dar el motivo => 422")
    void sinReemplazoExigeMotivo() {
        when(procedimientos.findById(10L)).thenReturn(Optional.of(f23));
        assertThatThrownBy(() -> service.crear(new VelorioRequest(10L, null, null, null, null, null, null, null, null, " "),
                autenticado(juez)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verify(velorios, never()).save(any());
    }

    @Test
    @DisplayName("Causa sugerida: % de aprobación, cantidad de objeciones y las 2 categorías más frecuentes")
    void causaSugerida() {
        when(resultadosProc.findFirstByProcedimientoIdOrderByCicloFechaFinDescIdDesc(10L)).thenReturn(Optional.of(
                new ResultadoProcedimiento(null, f23, new java.math.BigDecimal("0.1818"), 11, 17, Veredicto.VELORIO,
                        false, false, "m")));
        when(objeciones.findByProcedimientoId(10L)).thenReturn(List.of(
                new Objecion(10L, 3L, 1L, OrigenObjecion.VOTO, CategoriaObjecion.DUPLICA_OTRO_REGISTRO, null),
                new Objecion(10L, 3L, 1L, OrigenObjecion.VOTO, CategoriaObjecion.DUPLICA_OTRO_REGISTRO, null),
                new Objecion(10L, 3L, 2L, OrigenObjecion.VOTO, CategoriaObjecion.DEMASIADOS_PASOS, null),
                new Objecion(10L, 3L, 2L, OrigenObjecion.VOTO, CategoriaObjecion.OTRO, null)));

        assertThat(service.causaSugerida(f23)).isEqualTo(
                "18% de aprobación y 4 objeciones. Lo que más se repitió: duplica otro registro (2) y demasiados pasos (1).");
    }
}
