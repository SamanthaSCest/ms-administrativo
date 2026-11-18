package br.edu.imepac.clinicalapi.administrativo.repositories;

import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros dinâmicos comuns às listagens e buscas (Specification do Spring Data JPA).
 * Filtro vazio devolve {@code null}, que o {@code Specification.allOf} ignora.
 */
public final class Filtros {

    private Filtros() {
    }

    /** Sem situação informada, traz só os ativos (RF03). */
    public static <T> Specification<T> ativo(Boolean ativo) {
        boolean valor = ativo == null || ativo;
        return (root, query, cb) -> cb.equal(root.get("ativo"), valor);
    }

    /** Parte do nome, sem diferenciar maiúsculas. */
    public static <T> Specification<T> nomeContem(String nome) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        String padrao = "%" + nome.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("nome")), padrao);
    }

    /** CPF completo, só dígitos. */
    public static <T> Specification<T> cpfIgual(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("cpf"), cpf);
    }
}
