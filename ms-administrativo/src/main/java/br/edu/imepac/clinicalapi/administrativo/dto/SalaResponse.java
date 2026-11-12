package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.TipoSala;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class SalaResponse {

    private Long id;
    /** Identificador público (EntidadeBase). */
    private UUID uuid;
    private boolean ativo;
    private String criadoPor;
    private String atualizadoPor;
    private String numero;
    private TipoSala tipo;
    private String andar;
    private String observacoes;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;
}
