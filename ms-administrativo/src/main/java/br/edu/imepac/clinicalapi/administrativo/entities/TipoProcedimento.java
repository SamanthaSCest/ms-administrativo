package br.edu.imepac.clinicalapi.administrativo.entities;

/**
 * O que o item do cadastro de procedimentos representa. Exames usam o mesmo cadastro (código TUSS, especialidade,
 * duração, valor base, insumos e custos dos médicos), só que marcados como EXAME.
 */
public enum TipoProcedimento {
    /** Procedimento clínico ou cirúrgico (ex.: biópsia, sutura). Padrão. */
    PROCEDIMENTO,
    /** Exame (ex.: eletrocardiograma, hemograma, ultrassonografia). */
    EXAME
}
