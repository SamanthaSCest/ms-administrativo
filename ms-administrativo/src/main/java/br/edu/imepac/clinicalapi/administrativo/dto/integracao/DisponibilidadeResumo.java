package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import br.edu.imepac.clinicalapi.administrativo.entities.DiaSemana;
import br.edu.imepac.clinicalapi.administrativo.entities.TipoDisponibilidade;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Resumo da disponibilidade do médico (eventos; o ms-agendamento pode assinar disponibilidade.*). */
public record DisponibilidadeResumo(Long id, UUID uuid, TipoDisponibilidade tipo, Long medicoId, Long salaId, DiaSemana diaSemana, LocalDate data, @JsonFormat(pattern = "HH:mm") LocalTime horaInicial, @JsonFormat(pattern = "HH:mm") LocalTime horaFinal, Boolean disponivel, boolean ativo) {
}
