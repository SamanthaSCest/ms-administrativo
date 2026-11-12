package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Cargo;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO de SAÍDA: o que a API devolve ao cliente (nunca a entidade JPA).
 */
@Getter
@Setter
public class FuncionarioResponse extends ColaboradorResponse {

    private String matricula;
    private Cargo cargo;
    private String setor;
    private LocalDate dataAdmissao;
}
