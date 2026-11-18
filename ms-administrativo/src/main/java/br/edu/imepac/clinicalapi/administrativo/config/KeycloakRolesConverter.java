package br.edu.imepac.clinicalapi.administrativo.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Converte as client roles do Keycloak em authorities: cada item de
 * {@code resource_access.<clientId>.roles} (ex.: PACIENTE_GERENCIAR) vira uma authority com o mesmo nome,
 * que é o que o {@link SecurityConfig} checa. As roles vêm dos grupos do usuário (ou da service account).
 */
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    public static final String CLAIM_RESOURCE_ACCESS = "resource_access";

    private final String clientId;

    public KeycloakRolesConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(CLAIM_RESOURCE_ACCESS);
        if (resourceAccess == null || !(resourceAccess.get(clientId) instanceof Map<?, ?> client)
                || !(client.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(String::valueOf)
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r))
                .toList();
    }
}
