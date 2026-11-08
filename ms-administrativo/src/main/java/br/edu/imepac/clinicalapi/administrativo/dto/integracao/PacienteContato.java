package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

/** Dados de contato do paciente (usado pelo módulo de notificação). */
public record PacienteContato(String nome, String email, String telefone) {
}
