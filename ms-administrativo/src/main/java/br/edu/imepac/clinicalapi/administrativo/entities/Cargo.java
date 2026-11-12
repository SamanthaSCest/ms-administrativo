package br.edu.imepac.clinicalapi.administrativo.entities;

/**
 * Cargo do funcionário (diagrama: Enum Cargo). Define o nível de acesso do usuário:
 * ADMINISTRADOR -> perfil ADMIN; os demais -> perfil FUNCIONARIO.
 */
public enum Cargo {
    RECEPCIONISTA,
    ESTOQUISTA,
    RESPONSAVEL_FINANCEIRO,
    ADMINISTRADOR;

    public Perfil perfil() {
        return this == ADMINISTRADOR ? Perfil.ADMIN : Perfil.FUNCIONARIO;
    }
}
