package br.edu.imepac.clinicalapi.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Data e hora "atuais" sempre no fuso da clínica, independente do fuso do servidor.
 */
public final class Datas {

    public static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");

    private Datas() {
    }

    public static LocalDate hoje() {
        return LocalDate.now(ZONA_BRASIL);
    }

    public static LocalDateTime agora() {
        return LocalDateTime.now(ZONA_BRASIL);
    }
}
