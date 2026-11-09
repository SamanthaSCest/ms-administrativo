-- Migration Flyway: executada uma única vez, em ordem de versão (V1, V2...).
-- NUNCA edite uma migration já aplicada; crie uma nova (V2__...).
CREATE TABLE agendamentos (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo                        VARCHAR(31)  NOT NULL,
    medico_id                   BIGINT       NOT NULL,
    paciente_id                 BIGINT       NOT NULL,
    data_agendamento            DATETIME(6)  NOT NULL,
    status                      VARCHAR(20)  NOT NULL,
    observacoes                 VARCHAR(500),
    numero_carteira_plano_saude VARCHAR(50),
    sintomas                    VARCHAR(500),
    data_criacao                DATETIME(6)  NOT NULL,
    data_modificacao            DATETIME(6)  NOT NULL
);

CREATE INDEX idx_agendamentos_medico_data ON agendamentos (medico_id, data_agendamento);
CREATE INDEX idx_agendamentos_paciente ON agendamentos (paciente_id);
