package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.config.RabbitMqEventosConfiguration;
import br.edu.imepac.clinicalapi.administrativo.entities.OutboxEvento;
import br.edu.imepac.clinicalapi.administrativo.repositories.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
        when(outboxRepository.findTop100ByPublicadoEmIsNullOrderByCriadoEmAsc()).thenReturn(List.of(evento));
        confirmarEnvio(false);

        publicador.publicarPendentes();

        verify(rabbitTemplate).send(
                eq(RabbitMqEventosConfiguration.EXCHANGE_EVENTOS),
                eq("paciente.criado"),
                argThat((Message m) -> "application/json".equals(m.getMessageProperties().getContentType())
                        && m.getBody().length > 0),
                any(CorrelationData.class));
        verify(outboxRepository).save(evento);
        assertThat(evento.getPublicadoEm()).isNotNull();
    }

    @Test
    void naoDeveMarcarEventoPublicadoQuandoEnvioFalhar() {
        OutboxEvento evento = novoEvento();
        when(outboxRepository.findTop100ByPublicadoEmIsNullOrderByCriadoEmAsc()).thenReturn(List.of(evento));
        doThrow(new IllegalStateException("RabbitMQ indisponível"))
                .when(rabbitTemplate).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        assertThatThrownBy(publicador::publicarPendentes).isInstanceOf(IllegalStateException.class);

        verify(outboxRepository, never()).save(evento);
        assertThat(evento.getPublicadoEm()).isNull();
    }

    @Test
    void eventoSemFilaAssinanteNaoTravaAOutbox() {
        OutboxEvento evento = novoEvento();
        when(outboxRepository.findTop100ByPublicadoEmIsNullOrderByCriadoEmAsc()).thenReturn(List.of(evento));
        confirmarEnvio(true);

        publicador.publicarPendentes();

        verify(outboxRepository).save(evento);
        assertThat(evento.getPublicadoEm()).isNotNull();
    }

    /** Simula o RabbitMQ confirmando o envio; semFila = a mensagem volta por falta de fila assinante. */
    private void confirmarEnvio(boolean semFila) {
        doAnswer(invocacao -> {
            Message mensagem = invocacao.getArgument(2);
            CorrelationData confirmacao = invocacao.getArgument(3);
            if (semFila) {
                confirmacao.setReturned(new ReturnedMessage(mensagem, 312, "NO_ROUTE",
                        RabbitMqEventosConfiguration.EXCHANGE_EVENTOS, "paciente.criado"));
            }
            confirmacao.getFuture().complete(new CorrelationData.Confirm(true, ""));
            return null;
        }).when(rabbitTemplate).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));
    }

    private OutboxEvento novoEvento() {
        return new OutboxEvento(
                UUID.randomUUID().toString(),
                "paciente.criado",
                1L,
                new ObjectMapper().createObjectNode().put("nome", "Maria"),
                LocalDateTime.now());
    }
}
