package br.edu.imepac.clinicalapi.agendamento.config;

import br.edu.imepac.clinicalapi.common.security.ClientCredentialsTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configurações do cliente HTTP usado para falar com outros microserviços.
 */
@Configuration
public class RestClientConfig {

    private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

    /** Timeouts para que a indisponibilidade do ms-administrativo falhe rápido (503) em vez de travar. */
    @Bean
    public RestClientCustomizer restClientTimeouts(@Value("${clients.administrativo.connect-timeout:2s}") Duration connect,
                                                   @Value("${clients.administrativo.read-timeout:5s}") Duration read) {
        return builder -> builder.requestFactory(timeouts(connect, read));
    }

    /**
     * O ms-administrativo exige um token do Keycloak: este serviço usa o próprio client (ms-agendamento) com
     * client credentials, e a service account dele tem as roles de leitura (ver keycloak/realm-clinical.json).
     * Sem token-uri configurado (ex.: nos testes), as chamadas saem sem token.
     */
    @Bean
    public RestClientCustomizer restClientToken(@Value("${clients.keycloak.token-uri:}") String tokenUri,
                                                @Value("${clients.keycloak.client-id:ms-agendamento}") String clientId,
                                                @Value("${clients.keycloak.client-secret:}") String clientSecret,
                                                @Value("${clients.administrativo.connect-timeout:2s}") Duration connect,
                                                @Value("${clients.administrativo.read-timeout:5s}") Duration read) {
        if (tokenUri.isBlank()) {
            log.warn("clients.keycloak.token-uri não configurado: chamadas ao ms-administrativo vão sem token");
            return builder -> {
            };
        }
        // Builder próprio (e não o do Spring): o customizer não pode depender do builder que ele customiza
        ClientCredentialsTokenProvider tokens = new ClientCredentialsTokenProvider(
                RestClient.builder().requestFactory(timeouts(connect, read)), tokenUri, clientId, clientSecret);
        return builder -> builder.requestInterceptor(tokens.interceptor());
    }

    private static SimpleClientHttpRequestFactory timeouts(Duration connect, Duration read) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connect.toMillis());
        factory.setReadTimeout((int) read.toMillis());
        return factory;
    }
}
