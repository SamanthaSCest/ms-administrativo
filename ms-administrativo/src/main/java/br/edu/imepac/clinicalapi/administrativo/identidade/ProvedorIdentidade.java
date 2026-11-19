package br.edu.imepac.clinicalapi.administrativo.identidade;

import java.util.List;
import java.util.Set;

/**
 * Onde moram os usuários de acesso (senha, grupos e situação): o Keycloak ({@link KeycloakProvedorIdentidade}).
 * Os grupos são identificados pelo nome (ADMINISTRADOR, RECEPCIONISTA, ..., MEDICO).
 * Erros: login já existente -> ConflictException (409); grupo inexistente ou dado recusado (ex.: política de
 * senha) -> BusinessRuleException (422); provedor fora do ar -> ExternalServiceException (503).
 */
public interface ProvedorIdentidade {

    /** Cria o usuário já com senha e grupos. Devolve o id dele no provedor. */
    String criarUsuario(NovoUsuario usuario);

    /** Atualiza login e dados de exibição (nome, e-mail). */
    void atualizarDados(String id, String login, String nome, String email);

    void definirSenha(String id, String senha);

    void definirAtivo(String id, boolean ativo);

    /** Nomes dos grupos do usuário, em ordem alfabética. */
    List<String> grupos(String id);

    /** Substitui os grupos do usuário pelos informados. */
    void definirGrupos(String id, Set<String> grupos);

    /** Remove o usuário. Já removido não é erro. */
    void removerUsuario(String id);

    /** Nomes de todos os grupos do realm, em ordem alfabética. */
    List<String> listarGrupos();

    record NovoUsuario(String login, String senha, String nome, String email, boolean ativo, Set<String> grupos) {
    }
}
