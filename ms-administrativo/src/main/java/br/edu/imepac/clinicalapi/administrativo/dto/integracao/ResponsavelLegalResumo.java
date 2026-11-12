package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import java.util.UUID;

/** Resumo do responsável legal (eventos): CPF sempre mascarado (LGPD). */
public record ResponsavelLegalResumo(Long id, UUID uuid, String nome, String cpfMascarado, String parentesco, boolean ativo) {
}
