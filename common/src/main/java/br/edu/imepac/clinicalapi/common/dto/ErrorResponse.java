package br.edu.imepac.clinicalapi.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/** Formato único de erro devolvido por todos os microserviços. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
/**
 * DTO de SAÍDA: o que a API devolve ao cliente (nunca a entidade JPA).
 */
public class ErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final List<FieldDetail> details;

    public record FieldDetail(String field, String message) {
    }
}
