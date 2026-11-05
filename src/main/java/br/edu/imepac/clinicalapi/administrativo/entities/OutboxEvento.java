package br.edu.imepac.clinicalapi.administrativo.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_evento")
public class OutboxEvento {

    @Id
    private UUID id;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(name = "agregado_id", nullable = false)
    private UUID agregadoId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    protected OutboxEvento() {
    }

    public OutboxEvento(UUID id, String tipo, UUID agregadoId, JsonNode payload, Instant criadoEm) {
        this.id = id;
        this.tipo = tipo;
        this.agregadoId = agregadoId;
        this.payload = payload;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public UUID getAgregadoId() {
        return agregadoId;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getPublicadoEm() {
        return publicadoEm;
    }

    public void marcarPublicado(Instant publicadoEm) {
        this.publicadoEm = publicadoEm;
    }
}
