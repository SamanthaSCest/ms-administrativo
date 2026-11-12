package br.edu.imepac.clinicalapi.administrativo.entities;

import br.edu.imepac.clinicalapi.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Horário em que o médico atende, cadastrado pelo administrador. Pode ser semanal (DisponibilidadeDiaria)
 * ou uma exceção numa data (DisponibilidadeExcecao: folga ou horário extra).
 * SINGLE_TABLE: todas as subclasses na tabela disponibilidades_medicas, diferenciadas pela coluna "tipo".
 */
@Getter
@Setter
@Entity
@Table(name = "disponibilidades_medicas")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", length = 20)
public abstract class DisponibilidadeMedica extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    /** Sala de atendimento (opcional). */
    @ManyToOne
    @JoinColumn(name = "sala_id")
    private Sala sala;

    @Column(name = "hora_inicial", nullable = false)
    private LocalTime horaInicial;

    @Column(name = "hora_final", nullable = false)
    private LocalTime horaFinal;

    @Column(length = 255)
    private String observacoes;

    public boolean horarioValido() {
        return horaInicial != null && horaFinal != null && horaFinal.isAfter(horaInicial);
    }

    /** true se os dois intervalos de horário se cruzam (encostar no limite não conta). */
    public boolean sobrepoeHorario(DisponibilidadeMedica outra) {
        return horaInicial.isBefore(outra.getHoraFinal()) && outra.getHoraInicial().isBefore(horaFinal);
    }

    /** A disponibilidade vale para esta data? */
    public abstract boolean aplicaEm(LocalDate data);

    public abstract TipoDisponibilidade getTipo();
}
