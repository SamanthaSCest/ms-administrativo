package br.edu.imepac.clinicalapi.agendamento.client.dto;

/** Subconjunto da resposta de GET /pacientes/{id} do ms-administrativo. */
public record PacienteResumo(Long id, String nome) {
}
