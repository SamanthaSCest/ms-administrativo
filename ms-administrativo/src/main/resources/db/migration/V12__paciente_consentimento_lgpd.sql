-- Consentimento LGPD do paciente (RF26): se consentiu, guarda a versão do termo e quando consentiu.
ALTER TABLE pacientes ADD COLUMN consentimento_lgpd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pacientes ADD COLUMN versao_termo_lgpd VARCHAR(20);
ALTER TABLE pacientes ADD COLUMN data_consentimento_lgpd TIMESTAMP;
ALTER TABLE pacientes ADD CONSTRAINT ck_pacientes_consentimento_lgpd
    CHECK (consentimento_lgpd = FALSE OR (versao_termo_lgpd IS NOT NULL AND data_consentimento_lgpd IS NOT NULL));
