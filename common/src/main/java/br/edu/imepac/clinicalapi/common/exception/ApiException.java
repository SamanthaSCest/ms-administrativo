package br.edu.imepac.clinicalapi.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base das exceções de negócio. Cada subclasse define o HTTP status;
 * o {@code GlobalExceptionHandler} converte em {@code ErrorResponse}.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
