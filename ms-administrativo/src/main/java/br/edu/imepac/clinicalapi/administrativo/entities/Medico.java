package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Médico = Pessoa + CRM + Especialidade. Muitos médicos para uma especialidade (@ManyToOne).
 */
@Getter
@Setter
@Entity
@Table(name = "medicos")
public class Medico extends Pessoa {

    @Column(nullable = false, unique = true, length = 20)
    private String crm;

    // Vários médicos -> uma especialidade. optional=false => coluna NOT NULL
    @ManyToOne(optional = false)
    @JoinColumn(name = "especialidade_id", nullable = false)
    private Especialidade especialidade;
}
