package br.edu.imepac.clinicalapi.administrativo.dto;

import jakarta.validation.constraints.NotNull;

/** Ativa (true) ou inativa (false) um cadastro: PATCH /{recurso}/{id}/status. */
public record StatusRequest(@NotNull(message = "é obrigatório") Boolean ativo) {
}
