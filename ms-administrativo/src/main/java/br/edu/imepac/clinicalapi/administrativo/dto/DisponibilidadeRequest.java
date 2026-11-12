package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.DiaSemana;
import br.edu.imepac.clinicalapi.administrativo.entities.TipoDisponibilidade;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Disponibilidade do médico (cadastrada pelo ADMIN).
 * DIARIA exige diaSemana; EXCECAO exige data e disponivel (regras no service, 422).
 */
public record DisponibilidadeRequest(
        @NotNull(message = "é obrigatório") TipoDisponibilidade tipo,
        @NotNull(message = "é obrigatório") Long medicoId,
        Long salaId,
        @NotNull(message = "é obrigatório") @JsonFormat(pattern = "HH:mm") LocalTime horaInicial,
        @NotNull(message = "é obrigatório") @JsonFormat(pattern = "HH:mm") LocalTime horaFinal,
        @Size(max = 255, message = "deve ter no máximo 255 caracteres") String observacoes,
        DiaSemana diaSemana,
        LocalDate data,
        Boolean disponivel,
        @Size(max = 255, message = "deve ter no máximo 255 caracteres") String motivo) {
}
