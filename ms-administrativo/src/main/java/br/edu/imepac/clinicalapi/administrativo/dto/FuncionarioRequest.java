package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Perfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO de ENTRADA do funcionário: dados pessoais (PessoaRequest) + profissionais + perfil de acesso.
 */
@Getter
@Setter
public class FuncionarioRequest extends ColaboradorRequest {

    @NotBlank(message = "é obrigatório")
    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    private String matricula;

    @NotBlank(message = "é obrigatório")
    @Size(max = 100, message = "deve ter no máximo 100 caracteres")
    private String cargo;

    @Size(max = 100, message = "deve ter no máximo 100 caracteres")
    private String setor;

    @NotNull(message = "é obrigatório")
    @PastOrPresent(message = "não pode estar no futuro")
    private LocalDate dataAdmissao;

    /** ADMIN ou FUNCIONARIO. Se omitido, FUNCIONARIO. */
    private Perfil perfil;
}
