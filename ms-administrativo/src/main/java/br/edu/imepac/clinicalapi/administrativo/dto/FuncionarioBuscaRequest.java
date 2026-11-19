package br.edu.imepac.clinicalapi.administrativo.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Filtros da busca de funcionários (POST /funcionarios/busca). Vão no corpo, e não na URL,
 * porque nome e CPF são dados pessoais (RNF08). Todos os campos são opcionais.
 *
 * @param nome  parte do nome, sem diferenciar maiúsculas
 * @param cpf   CPF completo, só dígitos (traz no máximo um funcionário)
 * @param ativo situação; se omitido, traz só os ativos
 */
public record FuncionarioBuscaRequest(
        @Size(max = 150) String nome,
        @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos") String cpf,
        Boolean ativo) {
}
