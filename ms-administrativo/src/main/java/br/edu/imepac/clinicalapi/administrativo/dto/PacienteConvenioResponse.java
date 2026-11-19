package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.dto.integracao.ConvenioResumo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Saída do vínculo paciente–convênio. {@code vigente} é calculado para hoje (RN16). */
public record PacienteConvenioResponse(
        Long id,
        UUID uuid,
        Long pacienteId,
        ConvenioResumo convenio,
        String numeroCarteirinha,
        LocalDate validade,
        boolean ativo,
        boolean vigente,
        String criadoPor,
        String atualizadoPor,
        LocalDateTime dataCriacao,
        LocalDateTime dataModificacao) {
}
