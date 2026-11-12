package br.edu.imepac.clinicalapi.administrativo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InsumoRequest {

    @NotBlank(message = "é obrigatório")
    @Size(max = 30, message = "deve ter no máximo 30 caracteres")
    private String codigo;

    @NotBlank(message = "é obrigatório")
    @Size(max = 150, message = "deve ter no máximo 150 caracteres")
    private String nome;

    @Size(max = 255, message = "deve ter no máximo 255 caracteres")
    private String descricao;

    @NotBlank(message = "é obrigatório")
    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    private String unidadeMedida;

    @NotNull(message = "é obrigatório")
    @DecimalMin(value = "0.0", message = "não pode ser negativo")
    @Digits(integer = 8, fraction = 4, message = "até 8 dígitos inteiros e 4 decimais")
    private BigDecimal custoUnitario;
}
