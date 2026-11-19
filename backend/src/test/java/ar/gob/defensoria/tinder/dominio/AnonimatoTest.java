package ar.gob.defensoria.tinder.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/** El anonimato no es una promesa: es la estructura de datos. */
class AnonimatoTest {

    private static void sinUsuarioNiFecha(Class<?> clase, String... permitidos) {
        Field[] campos = clase.getDeclaredFields();
        assertThat(campos).isNotEmpty();
        for (Field f : campos) {
            if (Arrays.asList(permitidos).contains(f.getName())) {
                continue;
            }
            String nombre = f.getName().toLowerCase();
            Class<?> tipo = f.getType();
            assertThat(nombre).as("%s.%s", clase.getSimpleName(), f.getName())
                    .doesNotContain("usuario").doesNotContain("user").doesNotContain("empleado")
                    .doesNotContain("autor").doesNotContain("votante").doesNotContain("por")
                    .doesNotContain("fecha").doesNotContain("timestamp").doesNotContain("hora")
                    .doesNotContain("created").doesNotContain("updated").doesNotContain("time");
            assertThat(tipo).as("tipo de %s.%s", clase.getSimpleName(), f.getName())
                    .isNotEqualTo(Usuario.class)
                    .isNotEqualTo(VotoEmitido.class)
                    .isNotEqualTo(Date.class)
                    .isNotEqualTo(java.sql.Timestamp.class);
            assertThat(Temporal.class.isAssignableFrom(tipo)).as("%s.%s no puede ser fecha/hora", clase.getSimpleName(), f.getName())
                    .isFalse();
        }
    }

    @Test
    @DisplayName("La entidad Voto no tiene ningún campo de usuario ni de fecha/hora")
    void votoNoTieneUsuarioNiFecha() {
        sinUsuarioNiFecha(Voto.class);
        assertThat(Arrays.stream(Voto.class.getDeclaredFields()).map(Field::getName))
                .containsExactlyInAnyOrder("id", "procedimientoId", "cicloId", "areaId", "valor");
    }

    @Test
    @DisplayName("La entidad Objecion no tiene autor ni fecha/hora (solo guarda qué juez la evaluó)")
    void objecionNoTieneAutorNiFecha() {
        sinUsuarioNiFecha(Objecion.class, "evaluadaPorId");
        assertThat(Arrays.stream(Objecion.class.getDeclaredFields()).map(Field::getName))
                .containsExactlyInAnyOrder("id", "procedimientoId", "cicloId", "areaId", "origen", "categoria", "texto",
                        "fundada", "respuesta", "estado", "evaluadaPorId");
    }

    @Test
    @DisplayName("VotoEmitido registra quién votó pero no qué votó ni qué objetó")
    void votoEmitidoNoTieneValor() {
        assertThat(Arrays.stream(VotoEmitido.Id.class.getRecordComponents()).map(c -> c.getName()))
                .containsExactlyInAnyOrder("usuarioId", "procedimientoId", "cicloId");
        assertThat(Arrays.stream(VotoEmitido.class.getDeclaredFields()).map(Field::getType))
                .doesNotContain(ValorVoto.class, Voto.class, Objecion.class, CategoriaObjecion.class);
    }
}
