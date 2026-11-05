package br.edu.imepac.clinicalapi.administrativo.exceptions;

import org.springframework.http.HttpStatus;

public class NaoEncontradoException extends NegocioException {

    public NaoEncontradoException(String detalhe) {
        super(HttpStatus.NOT_FOUND, "ADM-GEN-404", "nao-encontrado",
                "Registro não encontrado", detalhe);
    }
}
