package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import br.edu.imepac.clinicalapi.administrativo.entities.TipoCustoMedico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Resumo do custo do médico (eventos). especialidadeId (CONSULTA) ou procedimentoId (PROCEDIMENTO). */
public record CustoMedicoResumo(Long id, UUID uuid, TipoCustoMedico tipo, Long medicoId, Long especialidadeId, Long procedimentoId, BigDecimal valor, LocalDate vigenciaInicio, LocalDate vigenciaFim, boolean ativo) {
}
