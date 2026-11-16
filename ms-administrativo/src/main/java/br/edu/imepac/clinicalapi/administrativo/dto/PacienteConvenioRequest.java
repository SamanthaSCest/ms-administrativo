package br.edu.imepac.clinicalapi.administrativo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Entrada do vínculo paciente–convênio (POST e PUT /pacientes/{id}/convenios).
 *
 * @param convenioId        convênio ativo (RN06)
 * @param numeroCarteirinha número da carteirinha do paciente no convênio
 * @param validade          validade da carteirinha; opcional, mas não pode estar vencida (RN07)
 */
public record PacienteConvenioRequest(
        @NotNull(message = "é obrigatório") Long convenioId,
        @NotBlank(message = "é obrigatório") @Size(max = 30, message = "deve ter no máximo 30 caracteres")
        String numeroCarteirinha,
        LocalDate validade) {
}
