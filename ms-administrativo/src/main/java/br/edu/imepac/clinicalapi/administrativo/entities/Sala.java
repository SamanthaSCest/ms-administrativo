package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Sala da clínica (consultório, cirurgia ou procedimento). Usada na disponibilidade dos médicos. */
@Getter
@Setter
@Entity
@Table(name = "salas")
public class Sala extends EntidadeBase {

    /** Número da sala/consultório (ex.: "101"). Único. */
    @Column(nullable = false, unique = true, length = 50)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSala tipo;

    @Column(length = 20)
    private String andar;

    @Column(length = 255)
    private String observacoes;
}
