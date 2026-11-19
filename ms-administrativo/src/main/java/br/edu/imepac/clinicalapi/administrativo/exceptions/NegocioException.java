package br.edu.imepac.clinicalapi.administrativo.exceptions;

import br.edu.imepac.clinicalapi.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Erro de negócio do MS-ADM, com código do catálogo (ex.: ADM-FUN-001).
 * {@code detalhes} vira campos extras no Problem Details (ex.: quantidade de médicos vinculados).
 */
public class NegocioException extends ApiException {

    private final String codigo;
    private final Map<String, Object> detalhes;

    public NegocioException(HttpStatus status, String codigo, String mensagem) {
        this(status, codigo, mensagem, Map.of());
    }

    public NegocioException(HttpStatus status, String codigo, String mensagem, Map<String, Object> detalhes) {
        super(status, mensagem);
        this.codigo = codigo;
        this.detalhes = Map.copyOf(detalhes);
    }

    public String getCodigo() {
        return codigo;
    }

    public Map<String, Object> getDetalhes() {
        return detalhes;
    }
}
