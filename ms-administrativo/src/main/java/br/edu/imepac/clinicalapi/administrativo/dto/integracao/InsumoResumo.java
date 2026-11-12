package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import java.math.BigDecimal;
import java.util.UUID;

/** Resumo do insumo (eventos; o ms-estoque assina insumo.*). */
public record InsumoResumo(Long id, UUID uuid, String codigo, String nome, String unidadeMedida, BigDecimal custoUnitario, boolean ativo) {
}
