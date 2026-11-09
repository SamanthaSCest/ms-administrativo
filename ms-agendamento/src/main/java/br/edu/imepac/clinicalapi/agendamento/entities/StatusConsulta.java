package br.edu.imepac.clinicalapi.agendamento.entities;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados possíveis de uma consulta.
 */
public enum StatusConsulta {
    AGENDADO, CANCELADO, REALIZADO, REMARCADO;

    /** Status em que a consulta ainda ocupa o horário do médico e pode ser alterada. */
    public static final Set<StatusConsulta> ATIVOS = EnumSet.of(AGENDADO, REMARCADO);

    public boolean isAtivo() {
        return ATIVOS.contains(this);
    }
}
