package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.validation.OnCreate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Entrada comum a quem acessa o sistema (médico e funcionário): dados pessoais + senha. */
@Getter
@Setter
public abstract class ColaboradorRequest extends PessoaRequest {

    /**
     * Senha de acesso (o login é o CPF). Obrigatória no cadastro; na atualização, se informada, troca a senha.
     * Só é gravada como hash BCrypt (limite de 72 bytes do algoritmo).
     */
    @NotBlank(groups = OnCreate.class, message = "é obrigatório")
    @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
    private String senha;
}
