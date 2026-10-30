package br.com.clinica.ms_administrativo.comum.validacao;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidator implements ConstraintValidator<Cnpj, String> {

    private static final int[] PESOS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext ctx) {
        // CNPJ é opcional em unidade e convênio: vazio passa
        if (valor == null || valor.isBlank()) {
            return true;
        }
        return valido(valor);
    }

    public static boolean valido(String cnpj) {
        if (cnpj == null || !cnpj.matches("\\d{14}")) {
            return false;
        }
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }
        return digito(cnpj, PESOS_1) == cnpj.charAt(12) - '0'
            && digito(cnpj, PESOS_2) == cnpj.charAt(13) - '0';
    }

    private static int digito(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}