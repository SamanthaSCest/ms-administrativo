package br.edu.imepac.clinicalapi.administrativo.dto;

/**
 * DTO de ENTRADA. As anotações (@NotBlank, @Size, @Past...) são validadas pelo Bean Validation
 * quando o controller usa @Valid; mensagens de erro saem no campo "details" da resposta 400.
 */
public class PacienteRequest extends PessoaRequest {
}
