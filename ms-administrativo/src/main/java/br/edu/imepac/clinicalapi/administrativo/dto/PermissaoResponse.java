package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;

/** Permissão do RBAC, para o administrador montar grupos e acessos. */
public record PermissaoResponse(Permissao codigo, String descricao) {

    public static PermissaoResponse de(Permissao permissao) {
        return new PermissaoResponse(permissao, permissao.getDescricao());
    }
}
