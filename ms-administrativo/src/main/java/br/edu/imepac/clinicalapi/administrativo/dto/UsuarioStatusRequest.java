package br.edu.imepac.clinicalapi.administrativo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Ativa/desativa o acesso de um usuário (somente ADMIN). */
@Getter
@Setter
public class UsuarioStatusRequest {

    @NotNull(message = "é obrigatório")
    private Boolean ativo;
}
