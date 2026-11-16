package br.edu.imepac.clinicalapi.administrativo.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Regras da data do consentimento LGPD (RF26). */
class PacienteConsentimentoTest {

    private static final LocalDateTime DIA_1 = LocalDateTime.of(2026, 1, 1, 10, 0);
    private static final LocalDateTime DIA_2 = LocalDateTime.of(2026, 2, 1, 10, 0);

    @Test
    void consentir_registraVersaoEData() {
        Paciente p = new Paciente();
        p.definirConsentimentoLgpd(true, " 1.0 ", DIA_1);

        assertThat(p.isConsentimentoLgpd()).isTrue();
        assertThat(p.getVersaoTermoLgpd()).isEqualTo("1.0");
        assertThat(p.getDataConsentimentoLgpd()).isEqualTo(DIA_1);
    }

    @Test
    void mesmaVersao_mantemDataOriginal() {
        Paciente p = new Paciente();
        p.definirConsentimentoLgpd(true, "1.0", DIA_1);
        p.definirConsentimentoLgpd(true, "1.0", DIA_2);

        assertThat(p.getDataConsentimentoLgpd()).isEqualTo(DIA_1);
    }

    @Test
    void versaoNova_renovaData() {
        Paciente p = new Paciente();
        p.definirConsentimentoLgpd(true, "1.0", DIA_1);
        p.definirConsentimentoLgpd(true, "2.0", DIA_2);

        assertThat(p.getVersaoTermoLgpd()).isEqualTo("2.0");
        assertThat(p.getDataConsentimentoLgpd()).isEqualTo(DIA_2);
    }

    @Test
    void retirar_limpaVersaoEData_eConsentirDeNovoRenova() {
        Paciente p = new Paciente();
        p.definirConsentimentoLgpd(true, "1.0", DIA_1);
        p.definirConsentimentoLgpd(false, null, DIA_2);

        assertThat(p.isConsentimentoLgpd()).isFalse();
        assertThat(p.getVersaoTermoLgpd()).isNull();
        assertThat(p.getDataConsentimentoLgpd()).isNull();

        p.definirConsentimentoLgpd(true, "1.0", DIA_2);
        assertThat(p.getDataConsentimentoLgpd()).isEqualTo(DIA_2);
    }
}
