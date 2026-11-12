package br.edu.imepac.clinicalapi.administrativo.entities;

import br.edu.imepac.clinicalapi.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Sala da clínica (consultório, exame...). Usada na disponibilidade dos médicos. */
@Getter
@Setter
@Entity
@Table(name = "salas")
public class Sala extends BaseEntity {

    /** Ex.: "Consultório 3". Única. */
    @Column(nullable = false, unique = true, length = 50)
    private String identificacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSala tipo;

    @Column(length = 20)
    private String andar;
}
