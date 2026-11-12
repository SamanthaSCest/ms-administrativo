package br.edu.imepac.clinicalapi.administrativo.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class InsumoResponse {

    private Long id;
    /** Identificador público (EntidadeBase). */
    private UUID uuid;
    private boolean ativo;
    private String criadoPor;
    private String atualizadoPor;
    private String codigo;
    private String nome;
    private String descricao;
    private String unidadeMedida;
    private BigDecimal custoUnitario;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;
}
