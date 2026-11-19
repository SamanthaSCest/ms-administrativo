package br.edu.imepac.clinicalapi.administrativo.dto;

import java.util.List;

/**
 * GET /auth/me: quem é o dono do token, lido do próprio token do Keycloak.
 * {@code permissoes} são as client roles do ms-administrativo (já somadas dos grupos): o front usa para
 * montar menus e telas.
 *
 * @param login      CPF (funcionário/médico) ou o username do Keycloak (ex.: admin)
 * @param nome       nome de exibição (claim "name")
 * @param grupos     grupos do Keycloak (claim "groups")
 * @param permissoes authorities do token
 */
public record UsuarioLogadoResponse(String login, String nome, String email, List<String> grupos,
                                    List<String> permissoes) {
}
