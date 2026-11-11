package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/** Valor que o médico cobra por um procedimento. */
@Getter
@Setter
@Entity
@DiscriminatorValue("PROCEDIMENTO")
public class CustoMedicoProcedimento extends CustoMedico {

    // nullable na tabela porque a coluna é compartilhada com CustoMedicoConsulta (SINGLE_TABLE)
    @ManyToOne
    @JoinColumn(name = "procedimento_id")
    private Procedimento procedimento;

    @Override
    public TipoCustoMedico getTipo() {
        return TipoCustoMedico.PROCEDIMENTO;
    }

    @Override
    public boolean mesmoItem(CustoMedico outro) {
        return outro instanceof CustoMedicoProcedimento p
                && Objects.equals(p.getProcedimento().getId(), procedimento.getId());
    }
}
