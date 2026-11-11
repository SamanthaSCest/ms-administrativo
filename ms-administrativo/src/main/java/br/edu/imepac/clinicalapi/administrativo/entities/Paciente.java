package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;

/**
 * Paciente = Pessoa + sexo, telefone fixo, endereço e responsável legal. Paciente não acessa o sistema.
 * O campo {@code telefone} herdado de Pessoa é o celular (diagrama: telefoneCelular).
 */
@Getter
@Setter
@Entity
@Table(name = "pacientes")
public class Paciente extends Pessoa {

    public static final int MAIORIDADE = 18;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sexo sexo;

    @Column(name = "telefone_fixo", length = 20)
    private String telefoneFixo;

    // Embutido: as colunas do endereço ficam na própria tabela pacientes
    @Embedded
    private Endereco endereco;

    // Vários pacientes -> um responsável (opcional; obrigatório para menores, regra no PacienteService)
    @ManyToOne
    @JoinColumn(name = "responsavel_id")
    private ResponsavelLegal responsavel;

    public int calcularIdade() {
        return calcularIdade(LocalDate.now());
    }

    public int calcularIdade(LocalDate referencia) {
        return Period.between(getDataNascimento(), referencia).getYears();
    }

    public boolean ehMenorDeIdade() {
        return calcularIdade() < MAIORIDADE;
    }
}
