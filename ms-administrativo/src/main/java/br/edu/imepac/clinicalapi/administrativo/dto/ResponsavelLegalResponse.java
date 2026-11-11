package br.edu.imepac.clinicalapi.administrativo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResponsavelLegalResponse {

    private Long id;
    private String nome;
    private String cpf;
    private String telefone;
    private String email;
    private String parentesco;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;
}
