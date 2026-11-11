package br.edu.imepac.clinicalapi.administrativo.exceptions;

import br.edu.imepac.clinicalapi.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Login ou senha inválidos (ou usuário inativo) -> 401 Unauthorized. */
public class CredenciaisInvalidasException extends ApiException {

    public CredenciaisInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "Login ou senha inválidos");
    }
}
