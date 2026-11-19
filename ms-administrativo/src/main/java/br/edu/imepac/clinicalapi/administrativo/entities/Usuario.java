package br.edu.imepac.clinicalapi.administrativo.entities;

import br.edu.imepac.clinicalapi.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Vínculo de um Médico ou Funcionário (Pessoa.usuario) com o seu usuário no Keycloak.
 * Senha, grupos e permissões ficam no Keycloak; aqui só o id de lá, o login (CPF) e a situação, que é
 * espelhada no "enabled" do Keycloak (usuário inativo não consegue fazer login).
 */
@Getter
@Setter
@Entity
@Table(name = "usuarios")
public class Usuario extends BaseEntity {

    /** O login é o CPF (username no Keycloak). */
    @Column(nullable = false, unique = true, length = 150)
    private String login;

    /** Id do usuário no Keycloak (UUID). Nulo só em registros anteriores ao Keycloak (migration V15). */
    @Column(name = "keycloak_id", unique = true, length = 36)
    private String keycloakId;

    @Column(nullable = false)
    private boolean ativo = true;
}
