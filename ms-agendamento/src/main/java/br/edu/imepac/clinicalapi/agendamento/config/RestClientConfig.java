package br.edu.imepac.clinicalapi.agendamento.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

/**
 * Configurações do cliente HTTP usado para falar com outros microserviços.
 */
@Configuration
public class RestClientConfig {

    /** Timeouts para que a indisponibilidade do ms-administrativo falhe rápido (503) em vez de travar. */
    @Bean
    public RestClientCustomizer restClientTimeouts(@Value("${clients.administrativo.connect-timeout:2s}") Duration connect,
                                                   @Value("${clients.administrativo.read-timeout:5s}") Duration read) {
        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout((int) connect.toMillis());
            factory.setReadTimeout((int) read.toMillis());
            builder.requestFactory(factory);
        };
    }

    /**
     * O ms-administrativo exige autenticação: chamadas entre serviços enviam a chave no header X-Api-Key
     * (lá vira o perfil SERVICO, com acesso de leitura a médicos e pacientes).
     */
    @Bean
    public RestClientCustomizer restClientApiKey(@Value("${clients.administrativo.api-key:}") String apiKey) {
        return builder -> {
            if (!apiKey.isBlank()) {
                builder.defaultHeader("X-Api-Key", apiKey);
            }
        };
    }
}
