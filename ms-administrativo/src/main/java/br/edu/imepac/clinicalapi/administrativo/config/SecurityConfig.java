package br.edu.imepac.clinicalapi.administrativo.config;

import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segurança do ms-administrativo (stateless, sem sessão). Quem autentica é o Keycloak (realm "clinical"):
 * <ul>
 *   <li>todas as rotas, menos as públicas, exigem {@code Authorization: Bearer <token>} emitido pelo Keycloak;</li>
 *   <li>o token é validado pela assinatura (chaves públicas do realm), expiração e emissor (iss);</li>
 *   <li>as client roles de {@code resource_access.ms-administrativo.roles} viram as authorities
 *       (ver {@link KeycloakRolesConverter}): são os nomes do enum {@link Permissao};</li>
 *   <li>usuários recebem as roles pelos grupos do Keycloak; outros microserviços (ex.: ms-agendamento) usam
 *       client credentials e recebem as roles da própria service account.</li>
 * </ul>
 * Contrato: static/contract/openapi-administrativo.yaml (seção "Autenticação").
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(KeycloakProperties.class)
public class SecurityConfig {

    /** Claim com o login (CPF para funcionários e médicos): vira o {@code authentication.getName()}. */
    public static final String CLAIM_LOGIN = "preferred_username";
    /** Nomes dos grupos do usuário (mapper "grupos" do client clinical-web). */
    public static final String CLAIM_GRUPOS = "groups";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, KeycloakProperties keycloak,
                                                   SegurancaErrorHandler errorHandler) throws Exception {
        http
                // CSRF desligado de propósito: a API não usa cookie nem sessão, só o header Authorization: Bearer,
                // que o navegador não envia sozinho; sem cookie, não há o que um site malicioso aproveitar.
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR java:S4502 - API stateless com token Bearer
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Público
                        .requestMatchers("/error", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/contract/**").permitAll()
                        // Qualquer usuário autenticado: dados do próprio usuário
                        .requestMatchers("/auth/**").authenticated()
                        // Usuários e a lista de grupos do Keycloak
                        .requestMatchers("/usuarios/**", "/grupos/**").hasAuthority(Permissao.USUARIO_GERENCIAR.name())
                        // Leituras para outros microserviços
                        .requestMatchers("/integracao/**").hasAuthority(Permissao.INTEGRACAO_CONSULTAR.name())
                        // As buscas são POST só para não pôr dado pessoal na URL (RNF08): são leitura
                        .requestMatchers(HttpMethod.POST, "/funcionarios/busca")
                        .hasAuthority(Permissao.FUNCIONARIO_CONSULTAR.name())
                        .requestMatchers(HttpMethod.POST, "/pacientes/busca")
                        .hasAuthority(Permissao.PACIENTE_CONSULTAR.name())
                        .requestMatchers(HttpMethod.POST, "/medicos/busca")
                        .hasAuthority(Permissao.MEDICO_CONSULTAR.name()))
                // Demais recursos: GET pede *_CONSULTAR; o resto (POST, PUT, PATCH, DELETE) pede *_GERENCIAR
                .authorizeHttpRequests(auth -> {
                    recurso(auth, Permissao.FUNCIONARIO_CONSULTAR, Permissao.FUNCIONARIO_GERENCIAR, "/funcionarios/**");
                    // Inclui os vínculos do paciente com convênio (/pacientes/{id}/convenios)
                    recurso(auth, Permissao.PACIENTE_CONSULTAR, Permissao.PACIENTE_GERENCIAR,
                            "/pacientes/**", "/responsaveis/**");
                    recurso(auth, Permissao.MEDICO_CONSULTAR, Permissao.MEDICO_GERENCIAR, "/medicos/**");
                    recurso(auth, Permissao.ESPECIALIDADE_CONSULTAR, Permissao.ESPECIALIDADE_GERENCIAR, "/especialidades/**");
                    recurso(auth, Permissao.CONVENIO_CONSULTAR, Permissao.CONVENIO_GERENCIAR, "/convenios/**");
                    recurso(auth, Permissao.SALA_CONSULTAR, Permissao.SALA_GERENCIAR, "/salas/**");
                    recurso(auth, Permissao.PROCEDIMENTO_CONSULTAR, Permissao.PROCEDIMENTO_GERENCIAR, "/procedimentos/**");
                    recurso(auth, Permissao.INSUMO_CONSULTAR, Permissao.INSUMO_GERENCIAR, "/insumos/**");
                    recurso(auth, Permissao.CUSTO_MEDICO_CONSULTAR, Permissao.CUSTO_MEDICO_GERENCIAR, "/custos-medicos/**");
                    recurso(auth, Permissao.DISPONIBILIDADE_CONSULTAR, Permissao.DISPONIBILIDADE_GERENCIAR,
                            "/disponibilidades/**");
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(keycloak)))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(e -> e.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler));
        return http.build();
    }

    /** GET (leitura) pede a permissão de consultar; os demais métodos, a de gerenciar. */
    private static void recurso(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            Permissao consultar, Permissao gerenciar, String... caminhos) {
        auth.requestMatchers(HttpMethod.GET, caminhos).hasAuthority(consultar.name());
        auth.requestMatchers(caminhos).hasAuthority(gerenciar.name());
    }

    static JwtAuthenticationConverter jwtAuthenticationConverter(KeycloakProperties keycloak) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRolesConverter(keycloak.clientId()));
        converter.setPrincipalClaimName(CLAIM_LOGIN);
        return converter;
    }

    /**
     * Chaves públicas buscadas no endereço configurado (interno no docker-compose), mas o emissor validado é o
     * público ({@code keycloak.issuer}): o Keycloak assina cada token com o mesmo iss (KC_HOSTNAME).
     * O JWKS só é baixado na primeira requisição autenticada (o serviço sobe mesmo com o Keycloak fora do ar).
     */
    @Bean
    public JwtDecoder jwtDecoder(KeycloakProperties keycloak) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(keycloak.jwkSetUri()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(keycloak.issuer()));
        return decoder;
    }
}
