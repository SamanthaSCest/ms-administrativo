package br.edu.imepac.clinicalapi.administrativo.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntidadeBaseTest {

    @Test
    void deveInativarEReativar() {
        EntidadeTeste entidade = new EntidadeTeste();

        assertTrue(entidade.isAtivo());

        entidade.inativar();
        assertFalse(entidade.isAtivo());

        entidade.reativar();
        assertTrue(entidade.isAtivo());
    }

    @Test
    void devePreencherDatasDeCriacaoEAtualizacao() {
        EntidadeTeste entidade = new EntidadeTeste();

        entidade.aoCriar();
        assertNotNull(entidade.getCriadoEm());
        assertNotNull(entidade.getAtualizadoEm());
        assertTrue(entidade.getCriadoEm().equals(entidade.getAtualizadoEm()));

        var criadoEm = entidade.getCriadoEm();
        entidade.aoAtualizar();
        assertFalse(entidade.getAtualizadoEm().isBefore(criadoEm));
    }

    private static class EntidadeTeste extends EntidadeBase {
    }
}
