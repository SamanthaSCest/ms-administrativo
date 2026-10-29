package br.com.clinica.ms_administrativo.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PacienteResumo(
        UUID id,
        String nome,
        String cpfMascarado,
        LocalDate dataNascimento,
        String sexo,
        boolean ativo
) {
}