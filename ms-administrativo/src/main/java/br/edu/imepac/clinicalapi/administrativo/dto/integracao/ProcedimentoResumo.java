package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import br.edu.imepac.clinicalapi.administrativo.entities.TipoProcedimento;
import java.math.BigDecimal;
import java.util.UUID;

/** Resumo do procedimento ou exame (eventos). */
public record ProcedimentoResumo(Long id, UUID uuid, TipoProcedimento tipo, String codigoTuss, String nome, Long especialidadeId, int duracaoMinutos, BigDecimal valorBase, boolean ativo) {
}
