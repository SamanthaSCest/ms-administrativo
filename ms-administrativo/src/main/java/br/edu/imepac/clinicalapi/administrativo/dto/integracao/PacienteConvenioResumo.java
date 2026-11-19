package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import java.time.LocalDate;
import java.util.UUID;

/** Vínculo paciente–convênio para a integração e o campo "dados" dos eventos paciente.convenio-*. */
public record PacienteConvenioResumo(
        Long id,
        UUID uuid,
        Long pacienteId,
        ConvenioResumo convenio,
        String numeroCarteirinha,
        LocalDate validade,
        boolean ativo) {
}
