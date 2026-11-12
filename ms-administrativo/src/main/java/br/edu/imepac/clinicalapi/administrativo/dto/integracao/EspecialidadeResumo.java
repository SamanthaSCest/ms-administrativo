package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import java.util.UUID;

/** Resumo da especialidade (integração e eventos). */
public record EspecialidadeResumo(Long id, UUID uuid, String nome, boolean ativo) {
}
