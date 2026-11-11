package br.edu.imepac.clinicalapi.administrativo.entities;

/**
 * Pessoa da equipe da clínica que acessa o sistema (Médico e Funcionário). O login do usuário é o CPF.
 * Paciente não implementa: é apenas um registro, sem acesso.
 */
public interface PossuiUsuario {

    String getCpf();

    Usuario getUsuario();

    void setUsuario(Usuario usuario);
}
