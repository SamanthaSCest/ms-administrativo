package br.edu.imepac.clinicalapi.administrativo.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record EnvelopeEvento(
        UUID eventoId,
        String tipo,
        int versao,
        Instant ocorridoEm,
        String origem,
        UUID agregadoId,
        List<String> camposAlterados,
        JsonNode dados
) {

    public EnvelopeEvento {
        Objects.requireNonNull(eventoId, "eventoId é obrigatório");
        Objects.requireNonNull(tipo, "tipo é obrigatório");
        Objects.requireNonNull(ocorridoEm, "ocorridoEm é obrigatório");
        Objects.requireNonNull(origem, "origem é obrigatória");
        Objects.requireNonNull(agregadoId, "agregadoId é obrigatório");
        camposAlterados = List.copyOf(Objects.requireNonNull(camposAlterados, "camposAlterados é obrigatório"));
        Objects.requireNonNull(dados, "dados é obrigatório");
    }
}
