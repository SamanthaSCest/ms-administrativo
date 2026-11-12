package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import br.edu.imepac.clinicalapi.administrativo.entities.Cargo;

import java.util.UUID;

/** Resumo do funcionário (eventos). Sem CPF. */
public record FuncionarioResumo(Long id, UUID uuid, String nome, String matricula, Cargo cargo, String setor, boolean ativo) {
}
