package br.edu.imepac.clinicalapi.administrativo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ConvenioResponse {

    private Long id;
    private String nome;
    private String registroAns;
    private String cnpj;
    private String telefone;
    private String email;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;
}
