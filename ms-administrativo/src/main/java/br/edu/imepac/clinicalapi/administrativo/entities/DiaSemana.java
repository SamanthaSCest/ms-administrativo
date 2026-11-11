package br.edu.imepac.clinicalapi.administrativo.entities;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Dia da semana da disponibilidade médica (diagrama: Enum DiaSemana). */
public enum DiaSemana {
    DOMINGO(DayOfWeek.SUNDAY),
    SEGUNDA(DayOfWeek.MONDAY),
    TERCA(DayOfWeek.TUESDAY),
    QUARTA(DayOfWeek.WEDNESDAY),
    QUINTA(DayOfWeek.THURSDAY),
    SEXTA(DayOfWeek.FRIDAY),
    SABADO(DayOfWeek.SATURDAY);

    private final DayOfWeek dayOfWeek;

    DiaSemana(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    /** true se a data cai neste dia da semana. */
    public boolean corresponde(LocalDate data) {
        return data.getDayOfWeek() == dayOfWeek;
    }
}
