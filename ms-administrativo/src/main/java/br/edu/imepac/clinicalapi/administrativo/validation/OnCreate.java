package br.edu.imepac.clinicalapi.administrativo.validation;

/**
 * Grupo de Bean Validation para regras que só valem no cadastro (POST), ex.: senha obrigatória.
 * Uso no controller: {@code @Validated({Default.class, OnCreate.class})}.
 */
public interface OnCreate {
}
