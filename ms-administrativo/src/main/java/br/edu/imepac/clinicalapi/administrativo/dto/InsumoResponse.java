package br.edu.imepac.clinicalapi.administrativo.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class InsumoResponse {

    private Long id;
    private String codigo;
    private String nome;
    private String descricao;
    private String unidadeMedida;
    private BigDecimal custoUnitario;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;
}
