package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderecoTest {

    private final Validator validator;

    EnderecoTest() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void deveAceitarCepComOitoDigitos() {
        Endereco endereco = new Endereco("12345678", null, null, null, null, null, UF.SP);

        assertTrue(validator.validate(endereco).isEmpty());
    }

    @Test
    void deveRejeitarCepForaDoFormato() {
        Endereco endereco = new Endereco("1234-567", null, null, null, null, null, UF.SP);

        assertFalse(validator.validate(endereco).isEmpty());
    }

    @Test
    void deveConterAsVinteESeteUnidadesFederativas() {
        assertEquals(27, UF.values().length);
    }
}
