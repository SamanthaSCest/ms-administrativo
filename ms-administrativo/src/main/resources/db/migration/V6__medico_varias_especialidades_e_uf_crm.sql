-- Médico conforme o diagrama: UF do CRM e várias especialidades (N:N).
-- Os vínculos atuais (medicos.especialidade_id) são copiados para a nova tabela antes de remover a coluna.
ALTER TABLE medicos ADD COLUMN uf_crm VARCHAR(2);

CREATE TABLE medico_especialidades (
    medico_id        BIGINT NOT NULL,
    especialidade_id BIGINT NOT NULL,
    CONSTRAINT pk_medico_especialidades PRIMARY KEY (medico_id, especialidade_id),
    CONSTRAINT fk_medico_especialidades_medico FOREIGN KEY (medico_id) REFERENCES medicos (id),
    CONSTRAINT fk_medico_especialidades_especialidade FOREIGN KEY (especialidade_id) REFERENCES especialidades (id)
);

INSERT INTO medico_especialidades (medico_id, especialidade_id)
SELECT id, especialidade_id FROM medicos;

ALTER TABLE medicos DROP CONSTRAINT fk_medicos_especialidade;
ALTER TABLE medicos DROP COLUMN especialidade_id;

-- CRM passa a ser único por UF (médicos antigos ficam com uf_crm NULL até serem atualizados)
ALTER TABLE medicos DROP CONSTRAINT uk_medicos_crm;
ALTER TABLE medicos ADD CONSTRAINT uk_medicos_crm_uf UNIQUE (crm, uf_crm);
