package br.edu.imepac.clinicalapi.administrativo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProcedimentoRequest {

    @NotNull(message = "é obrigatório")
    private Long especialidadeId;

    @NotBlank(message = "é obrigatório")
    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    private String codigoTuss;

    @NotBlank(message = "é obrigatório")
    @Size(max = 150, message = "deve ter no máximo 150 caracteres")
    private String nome;

    @Size(max = 255, message = "deve ter no máximo 255 caracteres")
    private String descricao;

    @NotNull(message = "é obrigatório")
    @Min(value = 1, message = "deve ser pelo menos 1")
    @Max(value = 1440, message = "deve ser no máximo 1440 (24 h)")
    private Integer duracaoMinutos;

    @NotNull(message = "é obrigatório")
    @DecimalMin(value = "0.0", message = "não pode ser negativo")
    @Digits(integer = 10, fraction = 2, message = "até 10 dígitos inteiros e 2 decimais")
    private BigDecimal valorBase;

    /** Insumos consumidos (opcional). Cada insumo só pode aparecer uma vez. */
    private List<@Valid @NotNull(message = "não pode ser nulo") ItemInsumo> insumos = new ArrayList<>();

    public record ItemInsumo(
            @NotNull(message = "é obrigatório") Long insumoId,
            @NotNull(message = "é obrigatório")
            @DecimalMin(value = "0.001", message = "deve ser maior que zero")
            @Digits(integer = 9, fraction = 3, message = "até 9 dígitos inteiros e 3 decimais")
            BigDecimal quantidade) {
    }
}
