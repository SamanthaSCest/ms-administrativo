package br.edu.imepac.clinicalapi.administrativo.config;

import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Segurança do ms-administrativo (stateless, sem sessão):
 * <ul>
 *   <li>{@code POST /auth/login} troca login/senha por um JWT (HS256) assinado por este serviço;</li>
 *   <li>as demais rotas exigem {@code Authorization: Bearer <token>}; cada permissão do claim "permissoes"
 *       (RBAC: grupos do usuário + permissões avulsas) vira uma authority;</li>
 *   <li>outros microserviços (ex.: ms-agendamento) se autenticam com o header {@code X-Api-Key} e recebem
 *       as permissões de {@link Permissao#DO_SERVICO}.</li>
 * </ul>
 * Contrato: static/contract/openapi-administrativo.yaml (seção "Autenticação").
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SegurancaProperties.class)
public class SecurityConfig {

    public static final String CLAIM_PERMISSOES = "permissoes";
    public static final String CLAIM_GRUPOS = "grupos";
    public static final String CLAIM_USUARIO_ID = "uid";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SegurancaProperties props,
                                                   SegurancaErrorHandler errorHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)          // API stateless com token: sem cookie de sessão
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Público
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers("/error", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/contract/**").permitAll()
                        // Qualquer usuário autenticado: dados do próprio usuário e troca de senha
                        .requestMatchers("/auth/**").authenticated()
                        // RBAC: usuários, grupos e permissões
                        .requestMatchers("/usuarios/**", "/grupos/**", "/permissoes/**")
                        .hasAuthority(Permissao.USUARIO_GERENCIAR.name())
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
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(e -> e.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
                // Não é @Component de propósito: senão o Spring Boot também o registraria como filtro de servlet
                .addFilterBefore(new ApiKeyAuthenticationFilter(props.integracao().apiKey()),
                        BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    /** GET (leitura) pede a permissão de consultar; os demais métodos, a de gerenciar. */
    private static void recurso(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            Permissao consultar, Permissao gerenciar, String... caminhos) {
        auth.requestMatchers(HttpMethod.GET, caminhos).hasAuthority(consultar.name());
        auth.requestMatchers(caminhos).hasAuthority(gerenciar.name());
    }

    /** Cada item do claim "permissoes" (ex.: PACIENTE_GERENCIAR) vira uma authority com o mesmo nome. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(CLAIM_PERMISSOES);
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(SegurancaProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(props)));
    }

    @Bean
    public JwtDecoder jwtDecoder(SegurancaProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave(props)).macAlgorithm(MacAlgorithm.HS256).build();
        // Valida expiração (exp/nbf) e se o token foi emitido por este serviço (iss)
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.jwt().emissor()));
        return decoder;
    }

    private static SecretKey chave(SegurancaProperties props) {
        String segredo = props.jwt().segredo();
        // HS256 exige chave de pelo menos 256 bits
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("seguranca.jwt.segredo (JWT_SECRET) deve ter pelo menos 32 bytes");
        }
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
