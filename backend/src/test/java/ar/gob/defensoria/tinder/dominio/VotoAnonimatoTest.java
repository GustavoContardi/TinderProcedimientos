package ar.gob.defensoria.tinder.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/** El anonimato no es una promesa: es la estructura de datos. */
class VotoAnonimatoTest {

    @Test
    @DisplayName("La entidad Voto no tiene ningún campo de usuario ni de fecha/hora")
    void votoNoTieneUsuarioNiFecha() {
        Field[] campos = Voto.class.getDeclaredFields();

        assertThat(campos).isNotEmpty();
        for (Field f : campos) {
            String nombre = f.getName().toLowerCase();
            Class<?> tipo = f.getType();
            assertThat(nombre).as("campo %s", f.getName())
                    .doesNotContain("usuario").doesNotContain("user").doesNotContain("empleado")
                    .doesNotContain("fecha").doesNotContain("timestamp").doesNotContain("hora")
                    .doesNotContain("created").doesNotContain("updated").doesNotContain("time");
            assertThat(tipo).as("tipo del campo %s", f.getName())
                    .isNotEqualTo(Usuario.class)
                    .isNotEqualTo(Participacion.class)
                    .isNotEqualTo(Date.class)
                    .isNotEqualTo(java.sql.Timestamp.class);
            assertThat(Temporal.class.isAssignableFrom(tipo)).as("%s no puede ser fecha/hora", f.getName()).isFalse();
        }
        assertThat(Arrays.stream(campos).map(Field::getName))
                .containsExactlyInAnyOrder("id", "procedimientoId", "cicloId", "areaId", "valor");
    }

    @Test
    @DisplayName("Participacion registra quién votó pero no qué votó")
    void participacionNoTieneValor() {
        assertThat(Arrays.stream(Participacion.Id.class.getRecordComponents()).map(c -> c.getName()))
                .containsExactlyInAnyOrder("usuarioId", "procedimientoId", "cicloId");
        assertThat(Arrays.stream(Participacion.class.getDeclaredFields()).map(Field::getType))
                .doesNotContain(ValorVoto.class, Voto.class);
    }
}
