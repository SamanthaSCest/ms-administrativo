package br.edu.imepac.clinicalapi.administrativo.utils;

/**
 * Mascara o CPF para os endpoints de integração (LGPD): só os dígitos do meio aparecem.
 * Ex.: 12345678909 -> ***.456.789-**
 */
public final class CpfMascara {

    private CpfMascara() {
    }

    public static String mascarar(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            return "***.***.***-**";
        }
        return "***.%s.%s-**".formatted(digitos.substring(3, 6), digitos.substring(6, 9));
    }
}
