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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Grupo de acesso do RBAC: um conjunto de permissões dado a vários usuários.
 * Os grupos padrão têm o nome do cargo (ADMINISTRADOR, RECEPCIONISTA, ESTOQUISTA, RESPONSAVEL_FINANCEIRO)
 * ou MEDICO, e o usuário entra neles automaticamente no cadastro. Grupo padrão não pode ser removido.
 */
@Getter
@Setter
@Entity
@Table(name = "grupos")
public class Grupo extends BaseEntity {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String MEDICO = "MEDICO";

    @Column(nullable = false, unique = true, length = 50)
    private String nome;

    @Column(length = 255)
    private String descricao;

    /** Criado pela migration; nasce com o nome de um cargo ou MEDICO. */
    @Column(nullable = false)
    private boolean padrao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "grupo_permissoes", joinColumns = @JoinColumn(name = "grupo_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permissao", nullable = false, length = 50)
    private Set<Permissao> permissoes = new HashSet<>();

    /** Troca o conjunto mantendo a mesma coleção gerenciada pelo JPA. */
    public void definirPermissoes(Set<Permissao> novas) {
        permissoes.clear();
        permissoes.addAll(novas);
    }

    public Set<Permissao> permissoesOrdenadas() {
        return permissoes.isEmpty() ? EnumSet.noneOf(Permissao.class) : EnumSet.copyOf(permissoes);
    }
}
