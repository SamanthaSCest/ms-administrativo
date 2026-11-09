package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.config.RabbitMqEventosConfiguration;
import br.edu.imepac.clinicalapi.administrativo.entities.OutboxEvento;
import br.edu.imepac.clinicalapi.administrativo.repositories.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A cada 5 segundos publica no RabbitMQ os eventos pendentes da outbox e marca publicado_em.
 * Desligado nos testes com outbox.publicador.habilitado=false.
 */
@Component
@ConditionalOnProperty(name = "outbox.publicador.habilitado", havingValue = "true", matchIfMissing = true)
public class PublicadorOutbox {

    private static final Logger log = LoggerFactory.getLogger(PublicadorOutbox.class);

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public PublicadorOutbox(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publicarPendentes() {
        for (OutboxEvento evento : outboxRepository.findTop100ByPublicadoEmIsNullOrderByCriadoEmAsc()) {
            MessageProperties propriedades = new MessageProperties();
            propriedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            Message mensagem = new Message(
                    evento.getPayload().toString().getBytes(StandardCharsets.UTF_8),
                    propriedades
            );

            CorrelationData confirmacao = new CorrelationData(evento.getId());
            rabbitTemplate.send(
                    RabbitMqEventosConfiguration.EXCHANGE_EVENTOS,
                    evento.getTipo(),
                    mensagem,
                    confirmacao
            );
            aguardarConfirmacao(confirmacao, evento);

            evento.marcarPublicado(LocalDateTime.now(ZoneOffset.UTC));
            outboxRepository.save(evento);
        }
    }

    private void aguardarConfirmacao(CorrelationData confirmacao, OutboxEvento evento) {
        try {
            CorrelationData.Confirm resultado = confirmacao.getFuture().get(10, TimeUnit.SECONDS);
            if (!resultado.isAck()) {
                throw new IllegalStateException("RabbitMQ rejeitou a publicação: " + resultado.getReason());
            }
            // Sem fila assinando esse tipo de evento o RabbitMQ devolve a mensagem. Não é erro: ninguém
            // precisava dela. Antes isso travava a outbox inteira (o mesmo evento era tentado para sempre).
            if (confirmacao.getReturned() != null) {
                log.warn("Evento {} ({}) sem fila assinante: {}", evento.getId(), evento.getTipo(),
                        confirmacao.getReturned().getReplyText());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Espera pela confirmação do RabbitMQ foi interrompida", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Falha ao aguardar confirmação do RabbitMQ", e.getCause());
        } catch (TimeoutException e) {
            throw new IllegalStateException("RabbitMQ não confirmou a publicação em 10 segundos", e);
        }
    }
}
