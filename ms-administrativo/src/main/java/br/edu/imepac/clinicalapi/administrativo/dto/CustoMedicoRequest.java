package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.TipoCustoMedico;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Custo do médico. CONSULTA exige especialidadeId; PROCEDIMENTO exige procedimentoId (regra no service, 422).
 * vigenciaFim é opcional (null = em aberto).
 */
public record CustoMedicoRequest(
        @NotNull(message = "é obrigatório") TipoCustoMedico tipo,
        @NotNull(message = "é obrigatório") Long medicoId,
        Long especialidadeId,
        Long procedimentoId,
        @NotNull(message = "é obrigatório")
        @DecimalMin(value = "0.0", message = "não pode ser negativo")
        @Digits(integer = 10, fraction = 2, message = "até 10 dígitos inteiros e 2 decimais")
        BigDecimal valor,
        @NotNull(message = "é obrigatório") LocalDate vigenciaInicio,
        LocalDate vigenciaFim) {
}
