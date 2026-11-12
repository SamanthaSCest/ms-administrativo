package br.edu.imepac.clinicalapi.administrativo.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Situação do cadastro após ativar/inativar. */
public record StatusResponse(String recurso, Long id, UUID uuid, boolean ativo, String atualizadoPor,
                             LocalDateTime dataModificacao) {
}
