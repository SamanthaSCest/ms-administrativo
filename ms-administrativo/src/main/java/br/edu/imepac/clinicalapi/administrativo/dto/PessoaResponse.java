package br.edu.imepac.clinicalapi.administrativo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de SAÍDA: o que a API devolve ao cliente (nunca a entidade JPA).
 */
@Getter
@Setter
public abstract class PessoaResponse {

    private Long id;
    /** Identificador público (EntidadeBase). */
    private UUID uuid;
    private boolean ativo;
    private String criadoPor;
    private String atualizadoPor;
    private String nome;
    private String cpf;
    private String email;
    private String telefone;
    private LocalDate dataNascimento;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;
}
