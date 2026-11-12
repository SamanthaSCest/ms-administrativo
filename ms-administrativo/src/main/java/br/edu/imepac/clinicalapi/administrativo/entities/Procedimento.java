package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Procedimento médico (código TUSS), de uma especialidade, com os insumos que consome.
 * Os InsumoProcedimento são composição: salvos e removidos junto com o procedimento.
 */
@Getter
@Setter
@Entity
@Table(name = "procedimentos")
public class Procedimento extends EntidadeBase {

    @ManyToOne(optional = false)
    @JoinColumn(name = "especialidade_id", nullable = false)
    private Especialidade especialidade;

    /** Código na tabela TUSS (Terminologia Unificada da Saúde Suplementar). Único. */
    @Column(name = "codigo_tuss", nullable = false, unique = true, length = 20)
    private String codigoTuss;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(name = "duracao_minutos", nullable = false)
    private int duracaoMinutos;

    @Column(name = "valor_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorBase;

    @OneToMany(mappedBy = "procedimento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InsumoProcedimento> insumos = new ArrayList<>();

    public void adicionarInsumo(Insumo insumo, BigDecimal quantidade) {
        InsumoProcedimento item = new InsumoProcedimento();
        item.setProcedimento(this);
        item.setInsumo(insumo);
        item.setQuantidade(quantidade);
        insumos.add(item);
    }

    /** Soma do custo de todos os insumos do procedimento. */
    public BigDecimal calcularCustoInsumos() {
        return insumos.stream()
                .map(InsumoProcedimento::calcularCusto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
