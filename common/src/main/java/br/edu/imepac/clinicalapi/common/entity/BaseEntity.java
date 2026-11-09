package br.edu.imepac.clinicalapi.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Campos comuns a todas as entidades (id e auditoria de datas). */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    // IDENTITY: o MySQL gera o id (AUTO_INCREMENT)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_modificacao", nullable = false)
    private LocalDateTime dataModificacao;

    // Callbacks do JPA: preenchem as datas automaticamente antes de inserir/atualizar
    @PrePersist
    void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        this.dataCriacao = agora;
        this.dataModificacao = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        this.dataModificacao = LocalDateTime.now();
    }
}
