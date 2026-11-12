package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import java.util.UUID;

/** Resumo do convênio (integração e eventos). Planos ficam com outro grupo. */
public record ConvenioResumo(Long id, UUID uuid, String nome, String registroAns, String cnpj, boolean ativo) {
}
