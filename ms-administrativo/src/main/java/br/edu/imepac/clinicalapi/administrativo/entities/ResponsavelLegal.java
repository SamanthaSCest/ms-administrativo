package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Responsável legal por pacientes menores de idade (um responsável para vários pacientes).
 * Não estende Pessoa: o diagrama não pede data de nascimento para o responsável.
 */
@Getter
@Setter
@Entity
@Table(name = "responsaveis_legais")
public class ResponsavelLegal extends EntidadeBase {

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(length = 20)
    private String telefone;

    @Column(length = 150)
    private String email;

    /** Ex.: MÃE, PAI, TUTOR. */
    @Column(nullable = false, length = 50)
    private String parentesco;
}
