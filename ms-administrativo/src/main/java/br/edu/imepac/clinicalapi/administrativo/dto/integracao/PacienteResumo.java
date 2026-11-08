package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import java.time.LocalDate;

/** Resumo do paciente para outros microserviços: CPF sempre mascarado (LGPD). */
public record PacienteResumo(Long id, String nome, String cpfMascarado, LocalDate dataNascimento) {
}
