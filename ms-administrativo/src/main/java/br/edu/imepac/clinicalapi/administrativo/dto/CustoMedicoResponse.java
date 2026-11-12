package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.TipoCustoMedico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** especialidade (CONSULTA) ou procedimento (PROCEDIMENTO) vem preenchido conforme o tipo. */
public record CustoMedicoResponse(
        Long id,
        TipoCustoMedico tipo,
        Referencia medico,
        Referencia especialidade,
        Referencia procedimento,
        BigDecimal valor,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        /** Vigente hoje? */
        boolean vigente,
        /** EntidadeBase: identificador público, status e auditoria. */
        UUID uuid,
        boolean ativo,
        String criadoPor,
        String atualizadoPor,
        LocalDateTime dataCriacao,
        LocalDateTime dataModificacao) {
}
