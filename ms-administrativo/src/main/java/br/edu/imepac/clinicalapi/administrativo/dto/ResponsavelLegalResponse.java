package br.edu.imepac.clinicalapi.administrativo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ResponsavelLegalResponse {

    private Long id;
    /** Identificador público (EntidadeBase). */
    private UUID uuid;
    private boolean ativo;
    private String criadoPor;
    private String atualizadoPor;
    private String nome;
    private String cpf;
    private String telefone;
    private String email;
    private String parentesco;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;
}
