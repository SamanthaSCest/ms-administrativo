package br.edu.imepac.clinicalapi.administrativo.entities;

/**
 * Cargo do funcionário (diagrama: Enum Cargo). Cada cargo tem um grupo de acesso padrão com o mesmo nome
 * (RBAC, migration V14): o funcionário entra nele no cadastro.
 */
public enum Cargo {
    RECEPCIONISTA,
    ESTOQUISTA,
    RESPONSAVEL_FINANCEIRO,
    ADMINISTRADOR
}
