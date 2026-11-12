package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import java.time.LocalDate;
import java.util.UUID;

/** Resumo do paciente para outros microserviços e eventos: CPF sempre mascarado (LGPD). */
public record PacienteResumo(Long id, String nome, String cpfMascarado, LocalDate dataNascimento,
                             UUID uuid, boolean ativo) {
}
