package ar.gob.defensoria.tinder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Parámetros del Tinder configurables en application.yml (prefijo "tinder"). */
@ConfigurationProperties(prefix = "tinder")
public record TinderProperties(
        int quorumMinimo,
        double umbralMatch,
        double umbralRechazo,
        int diasAvisoAniversario,
        int mesesAniversario,
        List<String> corsOrigins) {

    public TinderProperties {
        if (quorumMinimo <= 0) quorumMinimo = 3;
        if (umbralMatch <= 0) umbralMatch = 0.60;
        if (umbralRechazo <= 0) umbralRechazo = 0.60;
        if (diasAvisoAniversario <= 0) diasAvisoAniversario = 15;
        if (mesesAniversario <= 0) mesesAniversario = 6;
        if (corsOrigins == null || corsOrigins.isEmpty()) corsOrigins = List.of("http://localhost:4200");
    }
}
