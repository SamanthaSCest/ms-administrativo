package br.edu.imepac.clinicalapi.administrativo.dto;

import lombok.Getter;
import lombok.Setter;

/** Saída comum a médico e funcionário: dados pessoais + usuário de acesso (sem senha). */
@Getter
@Setter
public abstract class ColaboradorResponse extends PessoaResponse {

    /** Null só em médicos cadastrados antes da camada de autenticação. */
    private UsuarioResponse usuario;
}
