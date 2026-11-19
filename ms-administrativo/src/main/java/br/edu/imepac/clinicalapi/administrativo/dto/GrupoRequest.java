package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Cadastro/alteração de grupo de acesso (RBAC).
 *
 * @param nome       único; grupo padrão não pode ser renomeado
 * @param descricao  texto livre
 * @param permissoes permissões do grupo (substituem as atuais)
 */
public record GrupoRequest(
        @NotBlank(message = "é obrigatório") @Size(max = 50, message = "deve ter no máximo 50 caracteres") String nome,
        @Size(max = 255, message = "deve ter no máximo 255 caracteres") String descricao,
        @NotNull(message = "é obrigatório") Set<Permissao> permissoes) {
}
