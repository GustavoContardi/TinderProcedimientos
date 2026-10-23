package ar.gob.defensoria.tinder.web;

import ar.gob.defensoria.tinder.excepcion.ApiException;
import ar.gob.defensoria.tinder.web.dto.ErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/** Todos los errores salen como {codigo, mensaje} y en español. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorDTO> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErrorDTO(e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorDTO> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : f.getField() + " es inválido")
                .distinct()
                .collect(Collectors.joining(". "));
        return ResponseEntity.badRequest().body(new ErrorDTO("VALIDACION", mensaje));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ErrorDTO> requestInvalido(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorDTO("REQUEST_INVALIDO",
                "No entendimos el pedido: revisá los datos enviados."));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ResponseEntity<ErrorDTO> prohibido(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorDTO("PROHIBIDO", "Tu rol no tiene permiso para esto."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorDTO> integridad(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorDTO("CONFLICTO", "La operación choca con datos existentes (¿duplicado?)."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorDTO> noEncontrado(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorDTO("NO_ENCONTRADO", "Ese recurso no existe."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorDTO> inesperado(Exception e) {
        // Solo el tipo y el mensaje técnico: nunca datos del request (podría contener un voto).
        log.error("Error inesperado: {}", e.toString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDTO("ERROR_INTERNO", "Algo se rompió. No fue tu culpa (esta vez)."));
    }
}
