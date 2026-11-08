package br.edu.imepac.clinicalapi.common.exception;

import org.springframework.http.HttpStatus;

/** Falha ao se comunicar com outro microserviço. */
public class ExternalServiceException extends ApiException {

    public ExternalServiceException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
