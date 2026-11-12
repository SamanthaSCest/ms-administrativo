package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.validation.Cnpj;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConvenioRequest {

    @NotBlank(message = "é obrigatório")
    @Size(max = 150, message = "deve ter no máximo 150 caracteres")
    private String nome;

    @NotBlank(message = "é obrigatório")
    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    private String registroAns;

    @NotBlank(message = "é obrigatório")
    @Cnpj
    private String cnpj;

    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    private String telefone;

    @Email(message = "e-mail inválido")
    @Size(max = 150, message = "deve ter no máximo 150 caracteres")
    private String email;
}
