package ar.gob.defensoria.tinder.config;

import ar.gob.defensoria.tinder.repositorio.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AuthenticationEntryPoint entryPoint,
                                    AccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                // Entry point propio: devuelve JSON y NO manda "WWW-Authenticate", así el navegador no abre su popup.
                .httpBasic(b -> b.authenticationEntryPoint(entryPoint))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper) {
        return (request, response, ex) -> escribir(response, mapper, HttpServletResponse.SC_UNAUTHORIZED,
                "NO_AUTENTICADO", "Usuario o contraseña incorrectos. ¿Seguro que no es tu ex?");
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler(ObjectMapper mapper) {
        return (request, response, ex) -> escribir(response, mapper, HttpServletResponse.SC_FORBIDDEN,
                "PROHIBIDO", "Tu rol no tiene permiso para esto.");
    }

    private static void escribir(HttpServletResponse response, ObjectMapper mapper, int status,
                                 String codigo, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), Map.of("codigo", codigo, "mensaje", mensaje));
    }

    @Bean
    UserDetailsService userDetailsService(UsuarioRepository usuarios) {
        return email -> usuarios.findByEmailIgnoreCase(email)
                .map(UsuarioAutenticado::de)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario inexistente"));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(TinderProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(props.corsOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
