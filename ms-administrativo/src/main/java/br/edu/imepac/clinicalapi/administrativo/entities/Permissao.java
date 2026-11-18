package br.edu.imepac.clinicalapi.administrativo.entities;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Permissões do RBAC. Cada uma vira uma authority com o próprio nome (ex.: PACIENTE_GERENCIAR) e é checada
 * no SecurityConfig. A lista é fixa no código porque as regras de acesso a referenciam; o que o administrador
 * configura é quais permissões cada grupo (e cada usuário, de forma avulsa) tem.
 * Convenção: *_CONSULTAR libera as leituras (GET e as buscas por POST); *_GERENCIAR libera cadastrar, alterar,
 * ativar/inativar e remover.
 */
public enum Permissao {

    USUARIO_GERENCIAR("Gerenciar usuários, grupos e permissões de acesso"),
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

    /**
     * Permissões de outro microserviço autenticado por X-Api-Key (ex.: ms-agendamento, ms-estoque).
     * Não é um grupo: o serviço não tem usuário.
     */
    public static final Set<Permissao> DO_SERVICO = Collections.unmodifiableSet(EnumSet.of(
            PACIENTE_CONSULTAR, MEDICO_CONSULTAR, ESPECIALIDADE_CONSULTAR, CONVENIO_CONSULTAR, SALA_CONSULTAR,
            PROCEDIMENTO_CONSULTAR, INSUMO_CONSULTAR, INSUMO_GERENCIAR, CUSTO_MEDICO_CONSULTAR,
            DISPONIBILIDADE_CONSULTAR, INTEGRACAO_CONSULTAR));

    private final String descricao;

    Permissao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
