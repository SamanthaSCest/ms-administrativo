package br.edu.imepac.clinicalapi.administrativo.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfMascaraTest {

    @Test
    void deveMostrarSoOsDigitosDoMeio() {
        assertThat(CpfMascara.mascarar("12345678909")).isEqualTo("***.456.789-**");
    }

    @Test
    void deveAceitarCpfComMascara() {
        assertThat(CpfMascara.mascarar("123.456.789-09")).isEqualTo("***.456.789-**");
    }

    @Test
    void cpfForaDoFormato_naoExpoeNada() {
        assertThat(CpfMascara.mascarar("123")).isEqualTo("***.***.***-**");
        assertThat(CpfMascara.mascarar(null)).isNull();
    }
}
