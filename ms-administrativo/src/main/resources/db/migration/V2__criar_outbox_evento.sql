-- Outbox de eventos (RN19): o evento é gravado na mesma transação do cadastro
-- e publicado no RabbitMQ depois, por um agendador.
CREATE TABLE outbox_evento (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    tipo         VARCHAR(60)  NOT NULL,
    agregado_id  BIGINT       NOT NULL,
    payload      JSONB        NOT NULL,
    criado_em    TIMESTAMP(6) NOT NULL,
    publicado_em TIMESTAMP(6)
);

CREATE INDEX ix_outbox_pendente ON outbox_evento (publicado_em, criado_em);
