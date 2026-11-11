package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.UF;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Endereço (entrada e saída). O endereço é opcional, mas se for enviado precisa estar completo. */
@Getter
@Setter
public class EnderecoDto {

    @NotBlank(message = "é obrigatório")
    @Pattern(regexp = "\\d{8}", message = "deve conter 8 dígitos")
    private String cep;

    @NotBlank(message = "é obrigatório")
    @Size(max = 150, message = "deve ter no máximo 150 caracteres")
    private String logradouro;

    @NotBlank(message = "é obrigatório")
    @Size(max = 10, message = "deve ter no máximo 10 caracteres")
    private String numero;

    @Size(max = 60, message = "deve ter no máximo 60 caracteres")
    private String complemento;

    @NotBlank(message = "é obrigatório")
    @Size(max = 80, message = "deve ter no máximo 80 caracteres")
    private String bairro;

    @NotBlank(message = "é obrigatório")
    @Size(max = 80, message = "deve ter no máximo 80 caracteres")
    private String cidade;

    @NotNull(message = "é obrigatório")
    private UF uf;

    /** Só na saída: endereço em uma linha. */
    private String formatado;
}
