package br.com.clinica.ms_administrativo.eventos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class OutboxServiceTest {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void devePersistirEnvelopeCompletoNaOutbox() {
        UUID agregadoId = UUID.randomUUID();

        UUID eventoId = outboxService.registrar(
                "convenio.criado",
                agregadoId,
                Map.of("nome", "Plano Saúde"),
                List.of()
        );

        OutboxEvento evento = outboxRepository.findById(eventoId).orElseThrow();

        assertEquals("convenio.criado", evento.getTipo());
        assertEquals(agregadoId, evento.getAgregadoId());
        assertNotNull(evento.getCriadoEm());
        assertEquals(eventoId.toString(), evento.getPayload().path("eventoId").asText());
        assertEquals("convenio.criado", evento.getPayload().path("tipo").asText());
        assertEquals(1, evento.getPayload().path("versao").asInt());
        assertEquals("ms-administrativo", evento.getPayload().path("origem").asText());
        assertEquals(agregadoId.toString(), evento.getPayload().path("agregadoId").asText());
        assertEquals("Plano Saúde", evento.getPayload().path("dados").path("nome").asText());
        assertEquals(0, evento.getPayload().path("camposAlterados").size());
        assertNotNull(evento.getPayload().path("ocorridoEm").asText());
    }
}
