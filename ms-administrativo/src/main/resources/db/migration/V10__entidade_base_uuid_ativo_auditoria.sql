-- EntidadeBase do diagrama: uuid (identificador público adicional; a chave continua sendo o id BIGINT),
-- ativo (inativar/reativar) e auditoria (criado_por / atualizado_por = login de quem fez a operação).
-- Registros existentes recebem um UUID gerado pelo banco e ficam ativos.

ALTER TABLE especialidades ADD COLUMN uuid VARCHAR(36);
ALTER TABLE especialidades ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE especialidades ADD COLUMN criado_por VARCHAR(150);
ALTER TABLE especialidades ADD COLUMN atualizado_por VARCHAR(150);
UPDATE especialidades SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE especialidades ADD CONSTRAINT uk_especialidades_uuid UNIQUE (uuid);

ALTER TABLE medicos ADD COLUMN uuid VARCHAR(36);
ALTER TABLE medicos ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE medicos ADD COLUMN criado_por VARCHAR(150);
ALTER TABLE medicos ADD COLUMN atualizado_por VARCHAR(150);
UPDATE medicos SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE medicos ADD CONSTRAINT uk_medicos_uuid UNIQUE (uuid);

ALTER TABLE pacientes ADD COLUMN uuid VARCHAR(36);
ALTER TABLE pacientes ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE pacientes ADD COLUMN criado_por VARCHAR(150);
ALTER TABLE pacientes ADD COLUMN atualizado_por VARCHAR(150);
UPDATE pacientes SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE pacientes ADD CONSTRAINT uk_pacientes_uuid UNIQUE (uuid);

ALTER TABLE funcionarios ADD COLUMN uuid VARCHAR(36);
ALTER TABLE funcionarios ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE funcionarios ADD COLUMN criado_por VARCHAR(150);
ALTER TABLE funcionarios ADD COLUMN atualizado_por VARCHAR(150);
UPDATE funcionarios SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE funcionarios ADD CONSTRAINT uk_funcionarios_uuid UNIQUE (uuid);

ALTER TABLE convenios ADD COLUMN uuid VARCHAR(36);
ALTER TABLE convenios ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE convenios ADD COLUMN criado_por VARCHAR(150);
ALTER TABLE convenios ADD COLUMN atualizado_por VARCHAR(150);
UPDATE convenios SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE convenios ADD CONSTRAINT uk_convenios_uuid UNIQUE (uuid);

ALTER TABLE salas ADD COLUMN uuid VARCHAR(36);
ALTER TABLE salas ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE salas ADD COLUMN criado_por VARCHAR(150);
ALTER TABLE salas ADD COLUMN atualizado_por VARCHAR(150);
UPDATE salas SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE salas ADD CONSTRAINT uk_salas_uuid UNIQUE (uuid);

ALTER TABLE responsaveis_legais ADD COLUMN uuid VARCHAR(36);
ALTER TABLE responsaveis_legais ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE responsaveis_legais ADD COLUMN criado_por VARCHAR(150);
ALTER TABLE responsaveis_legais ADD COLUMN atualizado_por VARCHAR(150);
UPDATE responsaveis_legais SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE responsaveis_legais ADD CONSTRAINT uk_responsaveis_legais_uuid UNIQUE (uuid);

ALTER TABLE insumos ADD COLUMN uuid VARCHAR(36);
ALTER TABLE insumos ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE insumos ADD COLUMN criado_por VARCHAR(150);
ALTER TABLE insumos ADD COLUMN atualizado_por VARCHAR(150);
UPDATE insumos SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE insumos ADD CONSTRAINT uk_insumos_uuid UNIQUE (uuid);

ALTER TABLE procedimentos ADD COLUMN uuid VARCHAR(36);
ALTER TABLE procedimentos ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE procedimentos ADD COLUMN criado_por VARCHAR(150);
ALTER TABLE procedimentos ADD COLUMN atualizado_por VARCHAR(150);
UPDATE procedimentos SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE procedimentos ADD CONSTRAINT uk_procedimentos_uuid UNIQUE (uuid);

ALTER TABLE insumos_procedimento ADD COLUMN uuid VARCHAR(36);
ALTER TABLE insumos_procedimento ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE insumos_procedimento ADD COLUMN criado_por VARCHAR(150);
ALTER TABLE insumos_procedimento ADD COLUMN atualizado_por VARCHAR(150);
UPDATE insumos_procedimento SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE insumos_procedimento ADD CONSTRAINT uk_insumos_procedimento_uuid UNIQUE (uuid);

ALTER TABLE custos_medicos ADD COLUMN uuid VARCHAR(36);
ALTER TABLE custos_medicos ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE custos_medicos ADD COLUMN criado_por VARCHAR(150);
ALTER TABLE custos_medicos ADD COLUMN atualizado_por VARCHAR(150);
UPDATE custos_medicos SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE custos_medicos ADD CONSTRAINT uk_custos_medicos_uuid UNIQUE (uuid);

ALTER TABLE disponibilidades_medicas ADD COLUMN uuid VARCHAR(36);
ALTER TABLE disponibilidades_medicas ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE disponibilidades_medicas ADD COLUMN criado_por VARCHAR(150);
ALTER TABLE disponibilidades_medicas ADD COLUMN atualizado_por VARCHAR(150);
UPDATE disponibilidades_medicas SET uuid = UUID() WHERE uuid IS NULL;
ALTER TABLE disponibilidades_medicas ADD CONSTRAINT uk_disponibilidades_medicas_uuid UNIQUE (uuid);
