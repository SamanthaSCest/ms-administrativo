package br.edu.imepac.clinicalapi.agendamento.dto;

import br.edu.imepac.clinicalapi.agendamento.entities.StatusConsulta;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de SAÍDA: o que a API devolve ao cliente (nunca a entidade JPA).
 */
@Getter
@Setter
public class ConsultaResponse {

    private Long id;
    private Long medicoId;
    private Long pacienteId;
    private LocalDateTime dataAgendamento;
    private StatusConsulta status;
    private String sintomas;
    private String observacoes;
    private String numeroCarteiraPlanoSaude;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;
}
