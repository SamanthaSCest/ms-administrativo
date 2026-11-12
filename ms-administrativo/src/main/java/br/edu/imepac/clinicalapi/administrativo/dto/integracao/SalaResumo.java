package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import br.edu.imepac.clinicalapi.administrativo.entities.TipoSala;

import java.util.UUID;

/** Resumo da sala (integração e eventos). */
public record SalaResumo(Long id, UUID uuid, String numero, TipoSala tipo, String andar, String observacoes, boolean ativo) {
}
