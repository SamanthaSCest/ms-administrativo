package br.com.clinica.ms_administrativo.comum.validacao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadoresTest {

    @Test
    void cpfValido() {
        assertTrue(CpfValidator.valido("12345678909"));
        assertTrue(CpfValidator.valido("52998224725"));
    }

    @Test
    void cpfInvalido() {
        assertFalse(CpfValidator.valido("12345678900"));    // dígito verificador errado
        assertFalse(CpfValidator.valido("11111111111"));    // todos iguais
        assertFalse(CpfValidator.valido("123456789"));      // tamanho errado
        assertFalse(CpfValidator.valido("123.456.789-09")); // com máscara
        assertFalse(CpfValidator.valido(null));
    }

    @Test
    void cnpjValido() {
        assertTrue(CnpjValidator.valido("11222333000181"));
    }

    @Test
    void cnpjInvalido() {
        assertFalse(CnpjValidator.valido("11222333000182")); // dígito verificador errado
        assertFalse(CnpjValidator.valido("00000000000000")); // todos iguais
        assertFalse(CnpjValidator.valido("1122233300018"));  // tamanho errado
        assertFalse(CnpjValidator.valido(null));
    }
}