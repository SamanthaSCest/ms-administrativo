package br.edu.imepac.clinicalapi.administrativo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext ctx) {
        // Vazio é tratado por @NotBlank, para as mensagens não se misturarem
        if (valor == null || valor.isBlank()) {
            return true;
        }
        return valido(valor);
    }

    /** Também pode ser chamado direto pelos serviços (ex.: CPF do responsável legal). */
    public static boolean valido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}")) {
            return false;
        }
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digito(cpf, 9) == cpf.charAt(9) - '0'
            && digito(cpf, 10) == cpf.charAt(10) - '0';
    }

    private static int digito(String cpf, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}