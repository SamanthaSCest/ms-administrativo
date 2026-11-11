package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Perfil;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de SAÍDA do usuário. Nunca inclui a senha (nem o hash).
 */
@Getter
@Setter
public class UsuarioResponse {

    private Long id;
    private String login;
    private Perfil perfil;
    private boolean ativo;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;
}
