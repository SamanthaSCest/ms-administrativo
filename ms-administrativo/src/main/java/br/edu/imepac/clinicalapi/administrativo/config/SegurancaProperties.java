package br.edu.imepac.clinicalapi.administrativo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Propriedades {@code seguranca.*} do application.yml.
 *
 * @param jwt        segredo HMAC, emissor (claim "iss") e validade do token
 * @param admin      administrador criado na inicialização (ver {@link AdminInicializador})
 * @param integracao chave do header X-Api-Key usada por outros microserviços
 */
@ConfigurationProperties(prefix = "seguranca")
public record SegurancaProperties(Jwt jwt, Admin admin, Integracao integracao) {

    public record Jwt(String segredo, String emissor, Duration expiracao) {
    }

    public record Admin(String login, String senha) {
    }

    public record Integracao(String apiKey) {
    }
}
