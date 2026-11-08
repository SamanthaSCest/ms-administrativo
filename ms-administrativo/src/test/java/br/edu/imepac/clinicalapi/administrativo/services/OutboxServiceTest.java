package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.entities.OutboxEvento;
import br.edu.imepac.clinicalapi.administrativo.repositories.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Grava um evento de verdade na tabela outbox_evento (Flyway V2 em H2) e confere o envelope. */
@SpringBootTest
@ActiveProfiles("test")
class OutboxServiceTest {

    @Autowired
    private OutboxService outboxService;
    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void devePersistirEnvelopeCompletoNaOutbox() {
        UUID eventoId = outboxService.registrar("paciente.criado", 42L, Map.of("nome", "Maria"), List.of());

        OutboxEvento evento = outboxRepository.findById(eventoId.toString()).orElseThrow();

        assertThat(evento.getTipo()).isEqualTo("paciente.criado");
        assertThat(evento.getAgregadoId()).isEqualTo(42L);
        assertThat(evento.getCriadoEm()).isNotNull();
        assertThat(evento.getPublicadoEm()).isNull();
        assertThat(evento.getPayload().path("eventoId").asText()).isEqualTo(eventoId.toString());
        assertThat(evento.getPayload().path("versao").asInt()).isEqualTo(1);
        assertThat(evento.getPayload().path("origem").asText()).isEqualTo("ms-administrativo");
        assertThat(evento.getPayload().path("agregadoId").asLong()).isEqualTo(42L);
        assertThat(evento.getPayload().path("dados").path("nome").asText()).isEqualTo("Maria");
        assertThat(evento.getPayload().path("camposAlterados").size()).isZero();
    }

    @Test
    void eventoNovoApareceComoPendente() {
        UUID eventoId = outboxService.registrar("medico.criado", 7L, Map.of("crm", "123"), List.of());

        assertThat(outboxRepository.findTop100ByPublicadoEmIsNullOrderByCriadoEmAsc())
                .extracting(OutboxEvento::getId)
                .contains(eventoId.toString());
    }
}
