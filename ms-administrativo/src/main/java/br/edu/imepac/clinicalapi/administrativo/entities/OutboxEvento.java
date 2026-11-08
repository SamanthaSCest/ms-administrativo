package br.edu.imepac.clinicalapi.administrativo.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Evento gravado na MESMA transação do cadastro (padrão outbox). Um agendador publica os pendentes
 * no RabbitMQ depois, então nenhum evento se perde se o RabbitMQ estiver fora do ar.
 */
@Entity
@Table(name = "outbox_evento")
public class OutboxEvento {

    /** É o eventoId do envelope (UUID em texto), que os consumidores usam para ignorar repetidos. */
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(name = "agregado_id", nullable = false)
    private Long agregadoId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private JsonNode payload;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "publicado_em")
    private LocalDateTime publicadoEm;

    protected OutboxEvento() {
        // exigido pelo JPA
    }

    public OutboxEvento(String id, String tipo, Long agregadoId, JsonNode payload, LocalDateTime criadoEm) {
        this.id = id;
        this.tipo = tipo;
        this.agregadoId = agregadoId;
        this.payload = payload;
        this.criadoEm = criadoEm;
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public Long getAgregadoId() {
        return agregadoId;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getPublicadoEm() {
        return publicadoEm;
    }

    public void marcarPublicado(LocalDateTime publicadoEm) {
        this.publicadoEm = publicadoEm;
    }
}
