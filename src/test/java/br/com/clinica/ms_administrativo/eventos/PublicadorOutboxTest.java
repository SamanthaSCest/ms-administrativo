package br.com.clinica.ms_administrativo.eventos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicadorOutboxTest {

    private final OutboxRepository outboxRepository = mock(OutboxRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final PublicadorOutbox publicador = new PublicadorOutbox(outboxRepository, rabbitTemplate);

    @Test
    void devePublicarComoJsonEMarcarEventoPublicado() {
        OutboxEvento evento = novoEvento();
        when(outboxRepository.buscarPendentesParaPublicacao())
                .thenReturn(List.of(evento));
        doAnswer(invocacao -> {
            CorrelationData confirmacao = invocacao.getArgument(3);
            confirmacao.getFuture().complete(new CorrelationData.Confirm(true, ""));
            return null;
        }).when(rabbitTemplate).send(
                any(String.class),
                any(String.class),
                any(Message.class),
                any(CorrelationData.class)
        );

        publicador.publicarPendentes();

        verify(rabbitTemplate).send(
                eq(RabbitMqEventosConfiguration.EXCHANGE_EVENTOS),
                eq("convenio.criado"),
                org.mockito.ArgumentMatchers.argThat((Message mensagem) ->
                        mensagem.getMessageProperties().getContentType().equals("application/json")
                                && mensagem.getBody().length > 0),
                any(CorrelationData.class)
        );
        verify(outboxRepository).save(evento);
        assertNotNull(evento.getPublicadoEm());
    }

    @Test
    void naoDeveMarcarEventoPublicadoQuandoEnvioFalhar() {
        OutboxEvento evento = novoEvento();
        when(outboxRepository.buscarPendentesParaPublicacao())
                .thenReturn(List.of(evento));
        org.mockito.Mockito.doThrow(new IllegalStateException("RabbitMQ indisponível"))
                .when(rabbitTemplate)
                .send(
                        any(String.class),
                        any(String.class),
                        any(Message.class),
                        any(CorrelationData.class)
                );

        assertThrows(IllegalStateException.class, publicador::publicarPendentes);

        verify(outboxRepository, never()).save(evento);
        org.junit.jupiter.api.Assertions.assertNull(evento.getPublicadoEm());
    }

    @Test
    void naoDeveMarcarEventoPublicadoQuandoRabbitNaoRoteiaMensagem() {
        OutboxEvento evento = novoEvento();
        when(outboxRepository.buscarPendentesParaPublicacao())
                .thenReturn(List.of(evento));
        doAnswer(invocacao -> {
            CorrelationData confirmacao = invocacao.getArgument(3);
            Message mensagem = invocacao.getArgument(2);
            confirmacao.setReturned(new ReturnedMessage(
                    mensagem,
                    312,
                    "NO_ROUTE",
                    RabbitMqEventosConfiguration.EXCHANGE_EVENTOS,
                    "convenio.criado"
            ));
            confirmacao.getFuture().complete(new CorrelationData.Confirm(true, ""));
            return null;
        }).when(rabbitTemplate).send(
                any(String.class),
                any(String.class),
                any(Message.class),
                any(CorrelationData.class)
        );

        assertThrows(IllegalStateException.class, publicador::publicarPendentes);

        verify(outboxRepository, never()).save(evento);
        org.junit.jupiter.api.Assertions.assertNull(evento.getPublicadoEm());
    }

    private OutboxEvento novoEvento() {
        ObjectMapper objectMapper = new ObjectMapper();
        Instant agora = Instant.now();
        return new OutboxEvento(
                UUID.randomUUID(),
                "convenio.criado",
                UUID.randomUUID(),
                objectMapper.createObjectNode().put("nome", "Convênio exemplo"),
                agora
        );
    }
}
