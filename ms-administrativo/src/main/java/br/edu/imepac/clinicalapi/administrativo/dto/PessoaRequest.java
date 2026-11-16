package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Campos de entrada comuns a médico e paciente. */
@Getter
@Setter
public abstract class PessoaRequest {

    @NotBlank(message = "é obrigatório")
    @Size(max = 150, message = "deve ter no máximo 150 caracteres")
    private String nome;

    @NotBlank(message = "é obrigatório")
    @Cpf
    private String cpf;

    @Email(message = "e-mail inválido")
    @Size(max = 150, message = "deve ter no máximo 150 caracteres")
    private String email;

    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    private String telefone;

    @NotNull(message = "é obrigatório")
    @Past(message = "deve estar no passado")
    private LocalDate dataNascimento;
}