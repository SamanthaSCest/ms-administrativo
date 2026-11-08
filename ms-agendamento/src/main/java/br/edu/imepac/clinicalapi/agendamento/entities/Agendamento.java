package br.edu.imepac.clinicalapi.agendamento.entities;

import br.edu.imepac.clinicalapi.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Agendamento genérico (diagrama: abstract Agendamento). Hoje só existe {@link Consulta};
 * Retorno e Exame seguem o mesmo padrão (tabela única, discriminador "tipo").
 * Médico e paciente pertencem ao ms-administrativo, por isso guardamos apenas os ids.
 */
@Getter
@Setter
@Entity
@Table(name = "agendamentos")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
public abstract class Agendamento extends BaseEntity {

    // Guardamos só o id: médico e paciente são dados de OUTRO microserviço (outro banco),
    // então não existe @ManyToOne nem chave estrangeira entre eles.
    @Column(name = "medico_id", nullable = false)
    private Long medicoId;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "data_agendamento", nullable = false)
    private LocalDateTime dataAgendamento;

    // STRING grava o nome ("AGENDADO") em vez do número - mais seguro se a ordem do enum mudar
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConsulta status;

    @Column(length = 500)
    private String observacoes;

    @Column(name = "numero_carteira_plano_saude", length = 50)
    private String numeroCarteiraPlanoSaude;
}
