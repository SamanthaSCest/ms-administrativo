package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import java.math.BigDecimal;
import java.util.UUID;

/** Resumo do procedimento (eventos). */
public record ProcedimentoResumo(Long id, UUID uuid, String codigoTuss, String nome, Long especialidadeId, int duracaoMinutos, BigDecimal valorBase, boolean ativo) {
}
