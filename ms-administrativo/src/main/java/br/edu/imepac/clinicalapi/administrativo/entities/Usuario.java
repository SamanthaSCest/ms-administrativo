package br.edu.imepac.clinicalapi.administrativo.entities;

import br.edu.imepac.clinicalapi.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Credenciais de acesso. Médico e Funcionário apontam para um Usuario (Pessoa.usuario);
 * o administrador inicial existe sem pessoa vinculada.
 * Acesso por RBAC: o usuário tem as permissões dos seus grupos mais as permissões avulsas.
 */
@Getter
@Setter
@Entity
@Table(name = "usuarios")
public class Usuario extends BaseEntity {

    /** Para pessoas, o login é o CPF. */
    @Column(nullable = false, unique = true, length = 150)
    private String login;

    /** Hash BCrypt da senha (nunca exposto nos DTOs de saída). */
    @Column(nullable = false, length = 100)
    private String senha;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_grupos",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "grupo_id"))
    private Set<Grupo> grupos = new HashSet<>();

    /** Permissões dadas ao usuário fora dos grupos (exceções pontuais). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_permissoes", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permissao", nullable = false, length = 50)
    private Set<Permissao> permissoes = new HashSet<>();

    /** Permissões dos grupos + avulsas: é o que vai no token e o que o SecurityConfig checa. */
    public Set<Permissao> permissoesEfetivas() {
        Set<Permissao> todas = EnumSet.noneOf(Permissao.class);
        todas.addAll(permissoes);
        grupos.forEach(g -> todas.addAll(g.getPermissoes()));
        return todas;
    }

    public void adicionarGrupo(Grupo grupo) {
        grupos.add(grupo);
    }

    /** Remove pelo id (o Grupo não sobrescreve equals). */
    public void removerGrupo(Grupo grupo) {
        grupos.removeIf(g -> g.getId() != null && g.getId().equals(grupo.getId()));
    }

    public boolean pertenceAo(Grupo grupo) {
        return grupos.stream().anyMatch(g -> g.getId() != null && g.getId().equals(grupo.getId()));
    }

    /** Troca grupos e permissões avulsas mantendo as coleções gerenciadas pelo JPA. */
    public void definirAcessos(Set<Grupo> novosGrupos, Set<Permissao> novasPermissoes) {
        grupos.clear();
        grupos.addAll(novosGrupos);
        permissoes.clear();
        permissoes.addAll(novasPermissoes);
    }
}
