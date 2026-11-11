package br.edu.imepac.clinicalapi.administrativo.entities;

import br.edu.imepac.clinicalapi.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Credenciais de acesso. Médico, Paciente e Funcionário apontam para um Usuario (Pessoa.usuario);
 * o administrador inicial existe sem pessoa vinculada.
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(nullable = false)
    private boolean ativo = true;
}
