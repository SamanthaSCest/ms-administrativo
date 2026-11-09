package br.edu.imepac.clinicalapi.agendamento.client.dto;

/** Subconjunto da resposta de GET /medicos/{id} do ms-administrativo que interessa a este módulo. */
public record MedicoResumo(Long id, String nome, String crm) {
}
