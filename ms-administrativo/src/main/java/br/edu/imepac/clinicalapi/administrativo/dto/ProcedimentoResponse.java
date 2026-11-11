package br.edu.imepac.clinicalapi.administrativo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de SAÍDA do procedimento, com os insumos e o custo total deles (calcularCustoInsumos()).
 */
public record ProcedimentoResponse(
        Long id,
        EspecialidadeResponse especialidade,
        String codigoTuss,
        String nome,
        String descricao,
        int duracaoMinutos,
        BigDecimal valorBase,
        List<ItemInsumo> insumos,
        BigDecimal custoInsumos,
        LocalDateTime dataCriacao,
        LocalDateTime dataModificacao) {

    /** custo = custo unitário do insumo x quantidade. */
    public record ItemInsumo(InsumoResponse insumo, BigDecimal quantidade, BigDecimal custo) {
    }
}
