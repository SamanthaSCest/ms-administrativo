package br.edu.imepac.clinicalapi.administrativo.entities;

import br.edu.imepac.clinicalapi.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Convênio / plano de saúde aceito pela clínica. */
@Getter
@Setter
@Entity
@Table(name = "convenios")
public class Convenio extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String nome;

    /** Registro na ANS (Agência Nacional de Saúde Suplementar). */
    @Column(name = "registro_ans", nullable = false, unique = true, length = 20)
    private String registroAns;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(length = 20)
    private String telefone;

    @Column(length = 150)
    private String email;
}
