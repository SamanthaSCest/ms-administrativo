package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Sexo;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de SAÍDA: o que a API devolve ao cliente (nunca a entidade JPA).
 */
@Getter
@Setter
public class PacienteResponse extends PessoaResponse {

    private Sexo sexo;
    private String telefoneFixo;
    private EnderecoDto endereco;
    private ResponsavelLegalResponse responsavel;
    /** Calculados a partir da data de nascimento. */
    private int idade;
    private boolean menorDeIdade;
}
