package br.com.clinica.ms_administrativo.eventos;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Component
public class PublicadorOutbox {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public PublicadorOutbox(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publicarPendentes() {
        for (OutboxEvento evento : outboxRepository.buscarPendentesParaPublicacao()) {
            MessageProperties propriedades = new MessageProperties();
            propriedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            Message mensagem = new Message(
                    evento.getPayload().toString().getBytes(StandardCharsets.UTF_8),
                    propriedades
            );

            CorrelationData confirmacao = new CorrelationData(evento.getId().toString());
            rabbitTemplate.send(
                    RabbitMqEventosConfiguration.EXCHANGE_EVENTOS,
                    evento.getTipo(),
                    mensagem,
                    confirmacao
            );
            aguardarConfirmacao(confirmacao);

            evento.marcarPublicado(Instant.now());
            outboxRepository.save(evento);
        }
    }

    private void aguardarConfirmacao(CorrelationData confirmacao) {
        try {
            CorrelationData.Confirm resultado = confirmacao.getFuture().get(10, TimeUnit.SECONDS);
            if (!resultado.isAck()) {
                throw new IllegalStateException("RabbitMQ rejeitou a publicação: " + resultado.getReason());
            }
            if (confirmacao.getReturned() != null) {
                throw new IllegalStateException(
                        "RabbitMQ não roteou o evento: " + confirmacao.getReturned().getReplyText()
                );
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
