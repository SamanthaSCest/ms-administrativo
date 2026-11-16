package br.edu.imepac.clinicalapi.administrativo.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Vigência do vínculo paciente–convênio (RN16). */
class PacienteConvenioTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 6, 15);

    private static PacienteConvenio vinculo(LocalDate validade) {
        PacienteConvenio v = new PacienteConvenio();
        v.setConvenio(new Convenio());
        v.setValidade(validade);
        return v;
    }

    @Test
    void semValidade_vigente() {
        assertThat(vinculo(null).isVigente(HOJE)).isTrue();
    }

    @Test
    void validadeNoDia_vigente_diaSeguinte_naoVigente() {
        PacienteConvenio v = vinculo(HOJE);
        assertThat(v.isVigente(HOJE)).isTrue();
        assertThat(v.isVigente(HOJE.plusDays(1))).isFalse();
    }

    @Test
    void vinculoDesvinculado_naoVigente() {
        PacienteConvenio v = vinculo(null);
        v.inativar();
        assertThat(v.isVigente(HOJE)).isFalse();
    }

    @Test
    void convenioInativo_naoVigente() {
        PacienteConvenio v = vinculo(null);
        v.getConvenio().inativar();
        assertThat(v.isAtivo()).isTrue();
        assertThat(v.isVigente(HOJE)).isFalse();
    }
}
