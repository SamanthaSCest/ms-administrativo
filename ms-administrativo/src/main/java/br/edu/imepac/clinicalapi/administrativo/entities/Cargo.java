package br.edu.imepac.clinicalapi.administrativo.entities;

/**
 * Cargo do funcionário (diagrama: Enum Cargo). Cada cargo tem um grupo padrão no Keycloak com o mesmo nome
 * (keycloak/realm-clinical.json): o usuário do funcionário entra nele no cadastro.
 */
public enum Cargo {
    RECEPCIONISTA,
    ESTOQUISTA,
    RESPONSAVEL_FINANCEIRO,
    ADMINISTRADOR
}
