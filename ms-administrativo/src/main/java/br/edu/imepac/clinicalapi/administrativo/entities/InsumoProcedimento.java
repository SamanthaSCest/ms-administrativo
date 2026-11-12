package br.edu.imepac.clinicalapi.administrativo.entities;

import br.edu.imepac.clinicalapi.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Quantidade de um insumo consumida por um procedimento (composição: pertence ao Procedimento). */
@Getter
@Setter
@Entity
@Table(name = "insumos_procedimento")
public class InsumoProcedimento extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "procedimento_id", nullable = false)
    private Procedimento procedimento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;

    /** custo unitário do insumo x quantidade, com 2 casas. */
    public BigDecimal calcularCusto() {
        return insumo.getCustoUnitario().multiply(quantidade).setScale(2, RoundingMode.HALF_UP);
    }
}
