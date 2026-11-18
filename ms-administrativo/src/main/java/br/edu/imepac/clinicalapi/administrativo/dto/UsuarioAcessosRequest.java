package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;

import java.util.Set;

/**
 * Acessos do usuário (PUT /usuarios/{id}/acessos): substitui os grupos e as permissões avulsas.
 * Listas vazias (ou omitidas) tiram todos os grupos/permissões avulsas.
 *
 * @param grupoIds   ids dos grupos de acesso
 * @param permissoes permissões dadas fora dos grupos
 */
public record UsuarioAcessosRequest(Set<Long> grupoIds, Set<Permissao> permissoes) {
}
