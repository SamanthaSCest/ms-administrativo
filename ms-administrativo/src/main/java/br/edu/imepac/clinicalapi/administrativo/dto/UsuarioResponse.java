package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Usuario;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de SAÍDA do usuário de acesso. A senha fica só no Keycloak.
 * {@code grupos} (do Keycloak) só vem no detalhe ({@code GET /usuarios/{id}} e médico/funcionário por id);
 * nas listagens é omitido, para não consultar o Keycloak uma vez por linha.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsuarioResponse {

    private Long id;
    private String login;
    private String keycloakId;
    private boolean ativo;
    private List<String> grupos;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;

    /** Usado também pelo ModelMapper (ver ModelMapperConfig) quando o usuário vem dentro de médico/funcionário. */
    public static UsuarioResponse de(Usuario usuario) {
        UsuarioResponse r = new UsuarioResponse();
        r.setId(usuario.getId());
        r.setLogin(usuario.getLogin());
        r.setKeycloakId(usuario.getKeycloakId());
        r.setAtivo(usuario.isAtivo());
        r.setDataCriacao(usuario.getDataCriacao());
        r.setDataModificacao(usuario.getDataModificacao());
        return r;
    }

    public static UsuarioResponse de(Usuario usuario, List<String> grupos) {
        UsuarioResponse r = de(usuario);
        r.setGrupos(grupos);
        return r;
    }
}
