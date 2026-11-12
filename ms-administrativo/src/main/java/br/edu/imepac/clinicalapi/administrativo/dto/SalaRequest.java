package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.TipoSala;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalaRequest {

    @NotBlank(message = "é obrigatório")
    @Size(max = 50, message = "deve ter no máximo 50 caracteres")
    private String identificacao;

    @NotNull(message = "é obrigatório")
    private TipoSala tipo;

    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    private String andar;
}
