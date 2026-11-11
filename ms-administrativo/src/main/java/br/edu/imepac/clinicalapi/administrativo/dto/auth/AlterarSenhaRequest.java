package br.edu.imepac.clinicalapi.administrativo.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Troca de senha do próprio usuário autenticado. */
@Getter
@Setter
public class AlterarSenhaRequest {

    @NotBlank(message = "é obrigatório")
    private String senhaAtual;

    @NotBlank(message = "é obrigatório")
    @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
    private String novaSenha;
}
