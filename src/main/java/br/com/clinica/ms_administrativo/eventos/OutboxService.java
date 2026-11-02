package br.com.clinica.ms_administrativo.eventos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class OutboxService {

    private static final String ORIGEM = "ms-administrativo";
    private static final int VERSAO_INICIAL = 1;

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(
            OutboxRepository outboxRepository,
            @Qualifier("outboxObjectMapper") ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID registrar(String tipo, UUID agregadoId, Object dados, List<String> camposAlterados) {
        Objects.requireNonNull(tipo, "tipo é obrigatório");
        if (tipo.isBlank()) {
            throw new IllegalArgumentException("tipo não pode ser vazio");
        }
        Objects.requireNonNull(agregadoId, "agregadoId é obrigatório");
        Objects.requireNonNull(dados, "dados é obrigatório");
        Objects.requireNonNull(camposAlterados, "camposAlterados é obrigatório");

        Instant ocorridoEm = Instant.now();
        JsonNode dadosJson = objectMapper.valueToTree(dados);
        EnvelopeEvento envelope = new EnvelopeEvento(
                UUID.randomUUID(),
                tipo,
                VERSAO_INICIAL,
                ocorridoEm,
                ORIGEM,
                agregadoId,
                camposAlterados,
                dadosJson
        );
        outboxRepository.save(new OutboxEvento(
                envelope.eventoId(),
                envelope.tipo(),
                envelope.agregadoId(),
                objectMapper.valueToTree(envelope),
                envelope.ocorridoEm()
        ));
        return envelope.eventoId();
    }
}
