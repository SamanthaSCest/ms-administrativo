package br.edu.imepac.clinicalapi.administrativo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de ENTRADA. As anotações (@NotBlank, @Size, @Past...) são validadas pelo Bean Validation
 * quando o controller usa @Valid; mensagens de erro saem no campo "details" da resposta 400.
 */
@Getter
@Setter
public class MedicoRequest extends PessoaRequest {

    @NotBlank(message = "é obrigatório")
    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    private String crm;

    @NotNull(message = "é obrigatório")
    private Long especialidadeId;
}
