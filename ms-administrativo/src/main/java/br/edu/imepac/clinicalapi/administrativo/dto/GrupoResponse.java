package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Grupo;
import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;

import java.time.LocalDateTime;
import java.util.Set;

/** Grupo de acesso com as suas permissões. {@code padrao}: grupo de cargo/médico, não pode ser removido. */
public record GrupoResponse(Long id, String nome, String descricao, boolean padrao, Set<Permissao> permissoes,
                            LocalDateTime dataCriacao, LocalDateTime dataModificacao) {

    public static GrupoResponse de(Grupo grupo) {
        return new GrupoResponse(grupo.getId(), grupo.getNome(), grupo.getDescricao(), grupo.isPadrao(),
                grupo.permissoesOrdenadas(), grupo.getDataCriacao(), grupo.getDataModificacao());
    }
}
