package br.edu.imepac.clinicalapi.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * O gateway só deixa passar requisição com token válido do Keycloak (assinatura, expiração e emissor).
 * Ele NÃO checa permissões: o token segue no header Authorization e cada microserviço aplica as próprias regras.
 * Ficam públicos: health/info, Swagger UI e os contratos de cada serviço, e os preflights de CORS.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] PUBLICOS = {
            "/actuator/health/**", "/actuator/info",
            "/*/actuator/health/**", "/*/actuator/info",
            "/*/swagger-ui.html", "/*/swagger-ui/**", "/*/v3/api-docs/**", "/*/contract/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // CSRF desligado de propósito: sem cookie nem sessão, só o header Authorization: Bearer
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // NOSONAR java:S4502 - API stateless com token Bearer
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(PUBLICOS).permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }))
                .build();
    }

    /**
     * Chaves buscadas no endereço interno do Keycloak; emissor validado é o público (KC_HOSTNAME).
     * O JWKS só é baixado na primeira requisição com token.
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${keycloak.jwk-set-uri}") String jwkSetUri,
                                         @Value("${keycloak.issuer}") String issuer) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}
