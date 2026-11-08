package br.edu.imepac.clinicalapi.administrativo.entities;

import br.edu.imepac.clinicalapi.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidade JPA mapeada para a tabela "especialidades" (criada pela migration Flyway V1).
 */
@Getter
@Setter
@Entity
@Table(name = "especialidades")
public class Especialidade extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;
}
