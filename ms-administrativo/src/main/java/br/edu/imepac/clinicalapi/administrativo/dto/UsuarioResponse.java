package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;
import br.edu.imepac.clinicalapi.administrativo.entities.Usuario;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * DTO de SAÍDA do usuário. Nunca inclui a senha (nem o hash).
 * {@code permissoesEfetivas} = permissões dos grupos + avulsas: é o que o front usa para montar menus e telas.
 */
@Getter
@Setter
public class UsuarioResponse {

    private Long id;
    private String login;
    private boolean ativo;
    private List<GrupoResumo> grupos;
    /** Permissões avulsas (fora dos grupos). */
    private Set<Permissao> permissoes;
    private Set<Permissao> permissoesEfetivas;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;

    /** Usado também pelo ModelMapper (ver ModelMapperConfig) quando o usuário vem dentro de médico/funcionário. */
    public static UsuarioResponse de(Usuario usuario) {
        UsuarioResponse r = new UsuarioResponse();
        r.setId(usuario.getId());
        r.setLogin(usuario.getLogin());
        r.setAtivo(usuario.isAtivo());
        r.setGrupos(usuario.getGrupos().stream()
                .map(g -> new GrupoResumo(g.getId(), g.getNome()))
                .sorted(Comparator.comparing(GrupoResumo::nome))
                .toList());
        r.setPermissoes(usuario.getPermissoes().isEmpty()
                ? EnumSet.noneOf(Permissao.class) : EnumSet.copyOf(usuario.getPermissoes()));
        r.setPermissoesEfetivas(usuario.permissoesEfetivas());
        r.setDataCriacao(usuario.getDataCriacao());
        r.setDataModificacao(usuario.getDataModificacao());
        return r;
    }
}
