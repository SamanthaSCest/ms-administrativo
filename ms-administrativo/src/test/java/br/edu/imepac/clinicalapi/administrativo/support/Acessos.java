package br.edu.imepac.clinicalapi.administrativo.support;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Usuário de teste autenticado como se viesse do Keycloak, com as roles reais de um grupo
 * (keycloak/realm-clinical.json), por requisição:
 * {@code mockMvc.perform(get(...).with(Acessos.grupo("dr", "MEDICO")))}.
 * {@link #SERVICO} simula outro microserviço (client credentials do ms-agendamento).
 */
public final class Acessos {

    public static final String SERVICO = RealmClinical.SERVICO;

    private Acessos() {
    }

    public static RequestPostProcessor grupo(String username, String grupo) {
        return authentication(token(username, grupo));
    }

    /** Usuário com roles avulsas (fora dos grupos do realm), ex.: um grupo criado no Keycloak só para o teste. */
    public static RequestPostProcessor roles(String username, String... roles) {
        return authentication(token(username, List.of(), List.of(roles)));
    }

    /** Mesmo formato que o SecurityConfig produz para um token do Keycloak (nome = preferred_username). */
    static JwtAuthenticationToken token(String username, String grupo) {
        return token(username, SERVICO.equals(grupo) ? List.of() : List.of(grupo), RealmClinical.roles(grupo));
    }

    private static JwtAuthenticationToken token(String username, List<String> grupos, List<String> roles) {
        Jwt jwt = Jwt.withTokenValue("token-de-teste")
                .header("alg", "RS256")
                .subject(username)
                .claim("preferred_username", username)
                .claim("groups", grupos)
                .claim("resource_access", Map.of(RealmClinical.CLIENT, Map.of("roles", roles)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        return new JwtAuthenticationToken(jwt, authorities(roles), username);
    }

    private static Collection<GrantedAuthority> authorities(List<String> roles) {
        return roles.stream().map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r)).toList();
    }
}
