package br.edu.imepac.clinicalapi.administrativo.entities;

/**
 * Nível de acesso do usuário (somente equipe da clínica; paciente não acessa o sistema).
 * Vira a authority {@code ROLE_<perfil>} no Spring Security (ex.: ADMIN -> ROLE_ADMIN).
 */
public enum Perfil {
    /** Acesso total, inclusive cadastro de funcionários e gestão de usuários. */
    ADMIN,
    /** Equipe da clínica (recepção etc.): gerencia pacientes. */
    FUNCIONARIO,
    /** Consulta pacientes, médicos e especialidades. */
    MEDICO
}
