package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Funcionário = Pessoa (dados pessoais) + dados profissionais. O nível de acesso
 * (ADMIN ou FUNCIONARIO) fica no Usuario vinculado.
 */
@Getter
@Setter
@Entity
@Table(name = "funcionarios")
public class Funcionario extends Pessoa implements PossuiUsuario {

    @Column(nullable = false, unique = true, length = 20)
    private String matricula;

    @Column(nullable = false, length = 100)
    private String cargo;

    @Column(length = 100)
    private String setor;

    @Column(name = "data_admissao", nullable = false)
    private LocalDate dataAdmissao;

    // Credenciais de acesso. cascade ALL + orphanRemoval: o usuário nasce e morre junto com o funcionário.
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;
}
