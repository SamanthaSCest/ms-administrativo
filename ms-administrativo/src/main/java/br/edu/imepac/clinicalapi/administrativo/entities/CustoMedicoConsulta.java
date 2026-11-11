package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/** Valor da consulta do médico numa especialidade que ele atende. */
@Getter
@Setter
@Entity
@DiscriminatorValue("CONSULTA")
public class CustoMedicoConsulta extends CustoMedico {

    // nullable na tabela porque a coluna é compartilhada com CustoMedicoProcedimento (SINGLE_TABLE)
    @ManyToOne
    @JoinColumn(name = "especialidade_id")
    private Especialidade especialidade;

    @Override
    public TipoCustoMedico getTipo() {
        return TipoCustoMedico.CONSULTA;
    }

    @Override
    public boolean mesmoItem(CustoMedico outro) {
        return outro instanceof CustoMedicoConsulta c
                && Objects.equals(c.getEspecialidade().getId(), especialidade.getId());
    }
}
