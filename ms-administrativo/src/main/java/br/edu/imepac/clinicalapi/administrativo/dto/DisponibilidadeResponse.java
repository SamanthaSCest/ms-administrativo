package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.DiaSemana;
import br.edu.imepac.clinicalapi.administrativo.entities.TipoDisponibilidade;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** diaSemana (DIARIA) ou data/disponivel/motivo (EXCECAO) vêm preenchidos conforme o tipo. */
public record DisponibilidadeResponse(
        Long id,
        TipoDisponibilidade tipo,
        Referencia medico,
        Referencia sala,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicial,
        @JsonFormat(pattern = "HH:mm") LocalTime horaFinal,
        String observacoes,
        DiaSemana diaSemana,
        LocalDate data,
        Boolean disponivel,
        String motivo,
        LocalDateTime dataCriacao,
        LocalDateTime dataModificacao) {
}
