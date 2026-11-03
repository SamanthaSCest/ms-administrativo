package br.com.clinica.ms_administrativo.eventos;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RabbitMqEventosConfiguration {

    public static final String EXCHANGE_EVENTOS = "adm.events";

    @Bean
    TopicExchange exchangeEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }
}
