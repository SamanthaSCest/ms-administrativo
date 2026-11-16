package br.edu.imepac.clinicalapi.administrativo.exceptions;

import br.edu.imepac.clinicalapi.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Erro de negócio do MS-ADM, com código do catálogo (ex.: ADM-FUN-001). */
public class NegocioException extends ApiException {

    private final String codigo;

    public NegocioException(HttpStatus status, String codigo, String mensagem) {
        super(status, mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}