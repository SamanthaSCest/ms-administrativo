package br.edu.imepac.clinicalapi.administrativo.entities;

import br.edu.imepac.clinicalapi.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

/**
 * EntidadeBase do diagrama: acrescenta à BaseEntity (id Long + datas) o identificador público {@code uuid},
 * o status {@code ativo} e a auditoria de quem criou/alterou (login do token JWT, via Spring Data Auditing).
 * <p>
 * A chave primária continua sendo o {@code id} Long: o ms-agendamento guarda medicoId/pacienteId como BIGINT.
 * O uuid é uma coluna adicional e única. Fica neste módulo (e não no common) para não exigir migrations
 * no ms-agendamento, que também usa a BaseEntity.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class EntidadeBase extends BaseEntity {

    // VARCHAR(36) em vez do tipo UUID nativo: igual no MySQL e no H2 dos testes
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreatedBy
    @Column(name = "criado_por", updatable = false, length = 150)
    private String criadoPor;

    @LastModifiedBy
    @Column(name = "atualizado_por", length = 150)
    private String atualizadoPor;

    @PrePersist
    void gerarUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }
}
