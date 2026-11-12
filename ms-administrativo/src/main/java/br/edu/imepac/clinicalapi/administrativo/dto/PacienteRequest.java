package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Sexo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de ENTRADA. As anotações (@NotBlank, @Size, @Past...) são validadas pelo Bean Validation
 * quando o controller usa @Valid; mensagens de erro saem no campo "details" da resposta 400.
 * O campo {@code telefone} (de PessoaRequest) é o celular.
 */
@Getter
@Setter
public class PacienteRequest extends PessoaRequest {

    private Sexo sexo;

    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    private String telefoneFixo;

    // @Valid: valida também os campos internos do endereço (cep, uf...)
    @Valid
    private EnderecoDto endereco;

    /** Obrigatório se o paciente for menor de idade. */
    private Long responsavelId;
}
