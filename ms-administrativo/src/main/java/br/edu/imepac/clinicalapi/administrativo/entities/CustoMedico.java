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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Valor que o médico cobra (por consulta de uma especialidade ou por procedimento) num período de vigência.
 * SINGLE_TABLE: todas as subclasses na tabela custos_medicos, diferenciadas pela coluna "tipo".
 */
@Getter
@Setter
@Entity
@Table(name = "custos_medicos")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", length = 20)
public abstract class CustoMedico extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    /** null = vigência em aberto. */
    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    public boolean estaVigente(LocalDate data) {
        return !data.isBefore(vigenciaInicio) && (vigenciaFim == null || !data.isAfter(vigenciaFim));
    }

    /** true se os períodos de vigência dos dois custos se cruzam. */
    public boolean sobrepoe(LocalDate inicio, LocalDate fim) {
        boolean comecaAntesDoFimDoOutro = fim == null || !vigenciaInicio.isAfter(fim);
        boolean terminaDepoisDoInicioDoOutro = vigenciaFim == null || !vigenciaFim.isBefore(inicio);
        return comecaAntesDoFimDoOutro && terminaDepoisDoInicioDoOutro;
    }

    /** Tipo gravado na coluna discriminadora (CONSULTA ou PROCEDIMENTO). */
    public abstract TipoCustoMedico getTipo();

    /** Mesmo item de cobrança (mesma especialidade ou mesmo procedimento)? */
    public abstract boolean mesmoItem(CustoMedico outro);
}
