package br.edu.imepac.clinicalapi.agendamento.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de ENTRADA. As anotações (@NotBlank, @Size, @Past...) são validadas pelo Bean Validation
 * quando o controller usa @Valid; mensagens de erro saem no campo "details" da resposta 400.
 */
@Getter
@Setter
public class ConsultaRequest {

    @NotNull(message = "é obrigatório")
    private Long medicoId;

    @NotNull(message = "é obrigatório")
    private Long pacienteId;

    @NotNull(message = "é obrigatório")
    @Future(message = "deve ser uma data futura")
    private LocalDateTime dataAgendamento;

    @Size(max = 500, message = "deve ter no máximo 500 caracteres")
    private String sintomas;

    @Size(max = 500, message = "deve ter no máximo 500 caracteres")
    private String observacoes;

    @Size(max = 50, message = "deve ter no máximo 50 caracteres")
    private String numeroCarteiraPlanoSaude;
}
