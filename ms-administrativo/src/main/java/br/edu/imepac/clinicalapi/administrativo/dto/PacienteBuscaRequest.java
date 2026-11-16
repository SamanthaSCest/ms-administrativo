package br.edu.imepac.clinicalapi.administrativo.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Filtros da busca de pacientes (POST /pacientes/busca). Vão no corpo, e não na URL,
 * porque nome e CPF são dados pessoais (RNF08). Todos os campos são opcionais.
 *
 * @param nome  parte do nome, sem diferenciar maiúsculas
 * @param cpf   CPF completo, só dígitos (traz no máximo um paciente)
 * @param ativo situação; se omitido, traz só os ativos
 */
public record PacienteBuscaRequest(
        @Size(max = 150) String nome,
        @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos") String cpf,
        Boolean ativo) {
}
