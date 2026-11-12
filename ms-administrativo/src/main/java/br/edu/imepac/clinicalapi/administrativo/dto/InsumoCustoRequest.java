package br.edu.imepac.clinicalapi.administrativo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Atualização só do custo unitário (uso típico do ms-estoque após uma compra). */
public record InsumoCustoRequest(
        @NotNull(message = "é obrigatório")
        @DecimalMin(value = "0.0", message = "não pode ser negativo")
        @Digits(integer = 8, fraction = 4, message = "até 8 dígitos inteiros e 4 decimais")
        BigDecimal custoUnitario) {
}
