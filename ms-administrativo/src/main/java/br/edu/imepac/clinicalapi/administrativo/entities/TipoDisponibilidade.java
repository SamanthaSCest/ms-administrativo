package br.edu.imepac.clinicalapi.administrativo.entities;

/** Subtipos de DisponibilidadeMedica (valor da coluna discriminadora "tipo"). */
public enum TipoDisponibilidade {
    /** Repete toda semana no dia informado. */
    DIARIA,
    /** Vale só numa data: folga (disponivel = false) ou horário extra (disponivel = true). */
    EXCECAO
}
