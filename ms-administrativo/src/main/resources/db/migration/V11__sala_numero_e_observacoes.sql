-- Sala: "identificacao" vira "numero" (número do consultório/sala), entra "observacoes"
-- e o tipo passa a ser CONSULTORIO, CIRURGIA ou PROCEDIMENTO (EXAME deixa de existir).
ALTER TABLE salas ADD COLUMN numero VARCHAR(50);
UPDATE salas SET numero = identificacao;
ALTER TABLE salas MODIFY numero VARCHAR(50) NOT NULL;
ALTER TABLE salas DROP CONSTRAINT uk_salas_identificacao;
ALTER TABLE salas DROP COLUMN identificacao;
ALTER TABLE salas ADD CONSTRAINT uk_salas_numero UNIQUE (numero);

ALTER TABLE salas ADD COLUMN observacoes VARCHAR(255);

UPDATE salas SET tipo = 'PROCEDIMENTO' WHERE tipo = 'EXAME';