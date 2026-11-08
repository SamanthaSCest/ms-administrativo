package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

/**
 * Checagem rápida de existência. Como este módulo apaga fisicamente (DELETE), "ativo" é igual a "existe";
 * o campo fica no contrato para quando a inativação lógica for adotada.
 */
public record SituacaoResponse(boolean existe, boolean ativo) {
}
