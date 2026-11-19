package ar.gob.defensoria.tinder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Parámetros del Tinder configurables en application.yml (prefijo "tinder"). */
@ConfigurationProperties(prefix = "tinder")
public record TinderProperties(
        double umbralCasamiento,
        double umbralVelorio,
        int quorumArea,
        double participacionMinima,
        int diasAvisoAniversario,
        int mesesAniversario,
        int diasCompromisoMejora,
        int diasVotacionRapida,
        int diasHastaSegundaCita,
        List<String> corsOrigins) {

    public TinderProperties {
        if (umbralCasamiento <= 0) umbralCasamiento = 0.70;
        if (umbralVelorio <= 0) umbralVelorio = 0.40;
        if (quorumArea <= 0) quorumArea = 3;
        if (participacionMinima <= 0) participacionMinima = 0.50;
        if (diasAvisoAniversario <= 0) diasAvisoAniversario = 15;
        if (mesesAniversario <= 0) mesesAniversario = 6;
        if (diasCompromisoMejora <= 0) diasCompromisoMejora = 30;
        if (diasVotacionRapida <= 0) diasVotacionRapida = 7;
        if (diasHastaSegundaCita <= 0) diasHastaSegundaCita = 7;
        if (corsOrigins == null || corsOrigins.isEmpty()) corsOrigins = List.of("http://localhost:4200");
    }

    /** Valores por defecto (los mismos que application.yml). */
    public static TinderProperties porDefecto() {
        return new TinderProperties(0.70, 0.40, 3, 0.50, 15, 6, 30, 7, 7, List.of("http://localhost:4200"));
    }
}
