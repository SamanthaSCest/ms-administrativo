package br.edu.imepac.clinicalapi.administrativo.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO de SAÍDA: o que a API devolve ao cliente (nunca a entidade JPA).
 */
@Getter
@Setter
public class MedicoResponse extends PessoaResponse {

    private String crm;
    private EspecialidadeResponse especialidade;
}
