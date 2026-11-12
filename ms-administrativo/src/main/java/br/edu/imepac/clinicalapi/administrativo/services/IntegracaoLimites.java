package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;

import java.util.List;

/** Regras comuns da API de integração. */
final class IntegracaoLimites {

    /** Máximo de ids por busca em lote (RF22). */
    static final int MAX_IDS = 100;

    private IntegracaoLimites() {
    }

    static void validarLote(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessRuleException("Informe pelo menos um id");
        }
        if (ids.size() > MAX_IDS) {
            throw new BusinessRuleException("No máximo " + MAX_IDS + " ids por busca");
        }
    }
}
