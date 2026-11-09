package br.edu.imepac.clinicalapi.administrativo.entities;

import br.edu.imepac.clinicalapi.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Dados comuns de qualquer pessoa (diagrama: Abstract Pessoa). */
@Getter
@Setter
@MappedSuperclass
public abstract class Pessoa extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String nome;

    // unique=true documenta a restrição; o que vale de fato é a constraint criada na migration Flyway
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;
}
