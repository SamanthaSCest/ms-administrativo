package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.UF;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * DTO de ENTRADA. As anotações (@NotBlank, @Size, @Past...) são validadas pelo Bean Validation
 * quando o controller usa @Valid; mensagens de erro saem no campo "details" da resposta 400.
 */
@Getter
@Setter
public class MedicoRequest extends ColaboradorRequest {

    @NotBlank(message = "é obrigatório")
    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    private String crm;

    @NotNull(message = "é obrigatório")
    private UF ufCrm;

    /** Ids das especialidades que o médico atende (pelo menos uma). */
    @NotEmpty(message = "informe pelo menos uma especialidade")
    private Set<@NotNull(message = "não pode ser nulo") Long> especialidadeIds;
}
