package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Exceção numa data específica: folga/bloqueio (disponivel = false) ou horário extra (disponivel = true).
 */
@Getter
@Setter
@Entity
@DiscriminatorValue("EXCECAO")
public class DisponibilidadeExcecao extends DisponibilidadeMedica {

    @Column(name = "data_excecao")
    private LocalDate data;

    @Column
    private Boolean disponivel;

    @Column(length = 255)
    private String motivo;

    @Override
    public boolean aplicaEm(LocalDate data) {
        return data != null && data.equals(this.data);
    }

    @Override
    public TipoDisponibilidade getTipo() {
        return TipoDisponibilidade.EXCECAO;
    }
}
