package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.config.SecurityConfig;
import br.edu.imepac.clinicalapi.administrativo.dto.UsuarioLogadoResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Dados do usuário logado. Login, logout e troca de senha são feitos no Keycloak (realm "clinical").
 * Contrato: static/contract/openapi-administrativo.yaml
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** Lido do token (sem ir ao banco nem ao Keycloak): login, nome, grupos e permissões. */
    @GetMapping("/me")
    public UsuarioLogadoResponse me(Authentication authentication) {
        List<String> permissoes = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).sorted().toList();
        if (authentication instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            List<String> grupos = jwt.hasClaim(SecurityConfig.CLAIM_GRUPOS)
                    ? jwt.getClaimAsStringList(SecurityConfig.CLAIM_GRUPOS).stream().sorted().toList()
                    : List.of();
            return new UsuarioLogadoResponse(authentication.getName(), jwt.getClaimAsString("name"),
                    jwt.getClaimAsString("email"), grupos, permissoes);
        }
        return new UsuarioLogadoResponse(authentication.getName(), null, null, List.of(), permissoes);
    }
}
