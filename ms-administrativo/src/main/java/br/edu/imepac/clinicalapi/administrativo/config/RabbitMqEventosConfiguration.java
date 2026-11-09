package br.edu.imepac.clinicalapi.administrativo.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Exchange "adm.events" (tipo topic): a routing key é o nome do evento (ex.: paciente.criado).
 * Cada microserviço cria a sua fila e assina só o que precisa (ex.: paciente.* ou medico.inativado).
 */
@Configuration
@EnableScheduling
public class RabbitMqEventosConfiguration {

    public static final String EXCHANGE_EVENTOS = "adm.events";

    @Bean
    TopicExchange exchangeEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }
}
