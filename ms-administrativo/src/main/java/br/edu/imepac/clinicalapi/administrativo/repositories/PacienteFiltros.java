package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import org.springframework.data.jpa.domain.Specification;

/** Filtros dinâmicos da listagem de pacientes (Specification do Spring Data JPA). */
public final class PacienteFiltros {

    private PacienteFiltros() {
    }

    /** Sem situação informada, traz só os ativos (RF03). */
    public static Specification<Paciente> ativo(Boolean ativo) {
        boolean valor = ativo == null || ativo;
        return (root, query, cb) -> cb.equal(root.get("ativo"), valor);
    }

    public static Specification<Paciente> nomeContem(String nome) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        String padrao = "%" + nome.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("nome")), padrao);
    }

    public static Specification<Paciente> cpfIgual(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("cpf"), cpf);
    }
}
