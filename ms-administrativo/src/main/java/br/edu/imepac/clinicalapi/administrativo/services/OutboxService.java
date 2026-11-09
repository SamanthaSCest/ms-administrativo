package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.EnvelopeEvento;
import br.edu.imepac.clinicalapi.administrativo.entities.OutboxEvento;
import br.edu.imepac.clinicalapi.administrativo.repositories.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Registra um evento na outbox. Deve ser chamado DENTRO da transação do serviço que alterou o dado
 * (ex.: PacienteService.criar), para o evento só existir se o cadastro for confirmado (RN19).
 */
@Service
public class OutboxService {

    private static final String ORIGEM = "ms-administrativo";
    private static final int VERSAO_INICIAL = 1;

    private final OutboxRepository outboxRepository;
    // ObjectMapper configurado pelo Spring Boot (datas em ISO-8601, módulo de datas do Java)
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID registrar(String tipo, Long agregadoId, Object dados, List<String> camposAlterados) {
        Objects.requireNonNull(tipo, "tipo é obrigatório");
        if (tipo.isBlank()) {
            throw new IllegalArgumentException("tipo não pode ser vazio");
        }
        Objects.requireNonNull(agregadoId, "agregadoId é obrigatório");
        Objects.requireNonNull(dados, "dados é obrigatório");
        Objects.requireNonNull(camposAlterados, "camposAlterados é obrigatório");

        Instant ocorridoEm = Instant.now();
        EnvelopeEvento envelope = new EnvelopeEvento(
                UUID.randomUUID(),
                tipo,
                VERSAO_INICIAL,
                ocorridoEm,
                ORIGEM,
                agregadoId,
                camposAlterados,
                objectMapper.valueToTree(dados)
        );
        outboxRepository.save(new OutboxEvento(
                envelope.eventoId().toString(),
                envelope.tipo(),
                envelope.agregadoId(),
                objectMapper.valueToTree(envelope),
                LocalDateTime.ofInstant(ocorridoEm, ZoneOffset.UTC)
        ));
        return envelope.eventoId();
    }
}
