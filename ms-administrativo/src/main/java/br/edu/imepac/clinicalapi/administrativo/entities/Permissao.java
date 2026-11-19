package br.edu.imepac.clinicalapi.administrativo.entities;

/**
 * Permissões do RBAC. Cada uma é uma client role do client "ms-administrativo" no Keycloak, com o mesmo nome
 * (ex.: PACIENTE_GERENCIAR), e é checada no SecurityConfig. A lista é fixa no código porque as regras de acesso
 * a referenciam; quem recebe cada permissão é configurado no Keycloak (roles dos grupos e das service accounts,
 * ver keycloak/realm-clinical.json). Uma role nova precisa existir nos dois lugares.
 * Convenção: *_CONSULTAR libera as leituras (GET e as buscas por POST); *_GERENCIAR libera cadastrar, alterar,
 * ativar/inativar e remover.
 */
public enum Permissao {

    USUARIO_GERENCIAR("Gerenciar usuários e seus acessos"),
    FUNCIONARIO_CONSULTAR("Consultar funcionários"),
    FUNCIONARIO_GERENCIAR("Cadastrar, alterar e desativar funcionários"),
    MEDICO_CONSULTAR("Consultar médicos"),
    MEDICO_GERENCIAR("Cadastrar, alterar e desativar médicos"),
    ESPECIALIDADE_CONSULTAR("Consultar especialidades"),
    ESPECIALIDADE_GERENCIAR("Cadastrar, alterar e desativar especialidades"),
    PACIENTE_CONSULTAR("Consultar pacientes, responsáveis e convênios do paciente"),
    PACIENTE_GERENCIAR("Cadastrar e alterar pacientes, responsáveis e vínculos com convênio"),
    CONVENIO_CONSULTAR("Consultar convênios"),
    CONVENIO_GERENCIAR("Cadastrar, alterar e desativar convênios"),
    SALA_CONSULTAR("Consultar salas"),
    SALA_GERENCIAR("Cadastrar, alterar e desativar salas"),
    PROCEDIMENTO_CONSULTAR("Consultar procedimentos"),
    PROCEDIMENTO_GERENCIAR("Cadastrar, alterar e desativar procedimentos"),
    INSUMO_CONSULTAR("Consultar insumos"),
    INSUMO_GERENCIAR("Cadastrar e alterar insumos e seus custos"),
    CUSTO_MEDICO_CONSULTAR("Consultar custos dos médicos"),
    CUSTO_MEDICO_GERENCIAR("Cadastrar e alterar custos dos médicos"),
    DISPONIBILIDADE_CONSULTAR("Consultar disponibilidades dos médicos"),
    DISPONIBILIDADE_GERENCIAR("Cadastrar e alterar disponibilidades dos médicos"),
    INTEGRACAO_CONSULTAR("Consultar os endpoints de integração (/integracao)");

    private final String descricao;

    Permissao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
