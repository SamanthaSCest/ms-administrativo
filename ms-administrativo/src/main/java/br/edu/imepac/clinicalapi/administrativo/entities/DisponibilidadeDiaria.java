package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Disponibilidade que se repete toda semana (ex.: toda SEGUNDA das 08:00 às 12:00). */
@Getter
@Setter
@Entity
@DiscriminatorValue("DIARIA")
public class DisponibilidadeDiaria extends DisponibilidadeMedica {

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", length = 10)
    private DiaSemana diaSemana;

    @Override
    public boolean aplicaEm(LocalDate data) {
        return diaSemana != null && diaSemana.corresponde(data);
    }

    @Override
    public TipoDisponibilidade getTipo() {
        return TipoDisponibilidade.DIARIA;
    }
}
