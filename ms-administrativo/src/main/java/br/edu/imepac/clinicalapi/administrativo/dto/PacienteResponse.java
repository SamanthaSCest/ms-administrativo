package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Sexo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

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
    /** Consentimento LGPD (RF26); a data é registrada pelo servidor. */
    private boolean consentimentoLgpd;
    private String versaoTermoLgpd;
    private LocalDateTime dataConsentimentoLgpd;
    /**
     * Convênios vinculados (ativos), com carteirinha, validade e se está vigente hoje.
     * Só no GET por id; nas listagens e buscas fica de fora (o histórico completo está em /pacientes/{id}/convenios).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<PacienteConvenioResponse> convenios;
}
