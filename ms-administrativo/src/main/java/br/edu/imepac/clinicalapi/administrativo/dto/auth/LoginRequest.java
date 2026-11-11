package br.edu.imepac.clinicalapi.administrativo.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Credenciais do login. Para médicos, pacientes e funcionários o login é o CPF. */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "é obrigatório")
    private String login;

    @NotBlank(message = "é obrigatório")
    private String senha;
}
