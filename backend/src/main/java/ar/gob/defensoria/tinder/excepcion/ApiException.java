package ar.gob.defensoria.tinder.excepcion;

import org.springframework.http.HttpStatus;

/** Error de negocio que se traduce a {codigo, mensaje} con el status HTTP indicado. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje);
    }

    public static ApiException conflicto(String codigo, String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, codigo, mensaje);
    }

    public static ApiException reglaNegocio(String codigo, String mensaje) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, codigo, mensaje);
    }

    public static ApiException prohibido(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, "PROHIBIDO", mensaje);
    }

    public HttpStatus getStatus() { return status; }
    public String getCodigo() { return codigo; }
}
