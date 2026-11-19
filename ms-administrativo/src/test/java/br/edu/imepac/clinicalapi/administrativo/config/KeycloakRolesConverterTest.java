package br.edu.imepac.clinicalapi.administrativo.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRolesConverterTest {

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("t").header("alg", "RS256").subject("sub-uuid")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
    }

    @Test
    void soAsRolesDoClientMsAdministrativoViramAuthorities() {
        Jwt jwt = token()
                .claim("resource_access", Map.of(
                        "ms-administrativo", Map.of("roles", List.of("PACIENTE_CONSULTAR", "PACIENTE_GERENCIAR")),
                        "account", Map.of("roles", List.of("manage-account"))))
                .claim("realm_access", Map.of("roles", List.of("offline_access")))
                .build();

        assertThat(new KeycloakRolesConverter("ms-administrativo").convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("PACIENTE_CONSULTAR", "PACIENTE_GERENCIAR");
    }

    @Test
    void semRolesDoClient_nenhumaAuthority() {
        assertThat(new KeycloakRolesConverter("ms-administrativo").convert(token().claim("x", 1).build())).isEmpty();
        Jwt outroClient = token().claim("resource_access", Map.of("account", Map.of("roles", List.of("a")))).build();
        assertThat(new KeycloakRolesConverter("ms-administrativo").convert(outroClient)).isEmpty();
    }

    @Test
    void nomeDoUsuarioEhOPreferredUsername() {
        Jwt jwt = token()
                .claim("preferred_username", "11122233396")
                .claim("resource_access", Map.of("ms-administrativo", Map.of("roles", List.of("MEDICO_CONSULTAR"))))
                .build();
        KeycloakProperties props = new KeycloakProperties("http://kc", "clinical", "http://kc/realms/clinical",
                "ms-administrativo", "s");

        var auth = (JwtAuthenticationToken) SecurityConfig.jwtAuthenticationConverter(props).convert(jwt);

        assertThat(auth.getName()).isEqualTo("11122233396");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("MEDICO_CONSULTAR");
    }
}
