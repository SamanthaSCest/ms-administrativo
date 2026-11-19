package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Cargo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

/**
 * DTO de ENTRADA do funcionário: dados pessoais (PessoaRequest) + profissionais.
 * O cargo define o grupo de acesso padrão do usuário no Keycloak: o grupo com o mesmo nome do cargo.
 */
@Getter
@Setter
public class FuncionarioRequest extends ColaboradorRequest {

    @NotBlank(message = "é obrigatório")
    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    private String matricula;

    @NotNull(message = "é obrigatório")
    private Cargo cargo;

    @Size(max = 100, message = "deve ter no máximo 100 caracteres")
    private String setor;

    @NotNull(message = "é obrigatório")
    @PastOrPresent(message = "não pode estar no futuro")
    private LocalDate dataAdmissao;

    /**
     * Grupos de acesso do Keycloak (pelo nome) além do grupo do cargo (opcional). Exige a permissão
     * USUARIO_GERENCIAR.
     * No cadastro, o usuário fica no grupo do cargo + estes. Na atualização, se informado, substitui os grupos
     * extras (o do cargo continua); se omitido, os grupos ficam como estão.
     */
    private Set<String> grupos;
}
