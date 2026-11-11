package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.UF;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO de SAÍDA: o que a API devolve ao cliente (nunca a entidade JPA).
 */
@Getter
@Setter
public class MedicoResponse extends ColaboradorResponse {

    private String crm;
    private UF ufCrm;
    private List<EspecialidadeResponse> especialidades;
}
