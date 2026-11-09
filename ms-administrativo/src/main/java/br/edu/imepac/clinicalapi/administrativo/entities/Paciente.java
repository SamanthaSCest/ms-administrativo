package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Paciente: só os dados de Pessoa. Mapeado para a tabela "pacientes".
 */
@Entity
@Table(name = "pacientes")
public class Paciente extends Pessoa {
}
