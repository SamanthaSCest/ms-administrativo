package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Médico = Pessoa + CRM + Especialidade. Muitos médicos para uma especialidade (@ManyToOne).
 * Tem usuário de acesso com perfil MEDICO.
 */
@Getter
@Setter
@Entity
@Table(name = "medicos")
public class Medico extends Pessoa implements PossuiUsuario {

    @Column(nullable = false, unique = true, length = 20)
    private String crm;

    // Vários médicos -> uma especialidade. optional=false => coluna NOT NULL
    @ManyToOne(optional = false)
    @JoinColumn(name = "especialidade_id", nullable = false)
    private Especialidade especialidade;

    // Credenciais de acesso. cascade ALL + orphanRemoval: o usuário nasce e morre junto com o médico.
    // Pode ser null apenas em registros anteriores à migration V3.
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;
}
