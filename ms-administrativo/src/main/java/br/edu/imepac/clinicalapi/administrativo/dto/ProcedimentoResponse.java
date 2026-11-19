package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.TipoProcedimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

/**
 * DTO de SAÍDA do procedimento ou exame (tipo), com os insumos e o custo total deles (calcularCustoInsumos()).
 */
public record ProcedimentoResponse(
        Long id,
        EspecialidadeResponse especialidade,
        TipoProcedimento tipo,
        String codigoTuss,
        String nome,
        String descricao,
        int duracaoMinutos,
        BigDecimal valorBase,
        List<ItemInsumo> insumos,
        BigDecimal custoInsumos,
        /** EntidadeBase: identificador público, status e auditoria. */
        UUID uuid,
        boolean ativo,
        String criadoPor,
        String atualizadoPor,
        LocalDateTime dataCriacao,
        LocalDateTime dataModificacao) {

    /** custo = custo unitário do insumo x quantidade. */
    public record ItemInsumo(InsumoResponse insumo, BigDecimal quantidade, BigDecimal custo) {
    }
}
