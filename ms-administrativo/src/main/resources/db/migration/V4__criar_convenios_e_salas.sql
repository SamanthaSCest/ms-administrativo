-- Cadastros de apoio do diagrama: convênios e salas.
CREATE TABLE convenios (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(150) NOT NULL,
    registro_ans     VARCHAR(20)  NOT NULL,
    cnpj             VARCHAR(14)  NOT NULL,
    telefone         VARCHAR(20),
    email            VARCHAR(150),
    data_criacao     DATETIME(6)  NOT NULL,
    data_modificacao DATETIME(6)  NOT NULL,
    CONSTRAINT uk_convenios_registro_ans UNIQUE (registro_ans),
    CONSTRAINT uk_convenios_cnpj UNIQUE (cnpj)
);

CREATE TABLE salas (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    identificacao    VARCHAR(50)  NOT NULL,
    tipo             VARCHAR(20)  NOT NULL,  -- CONSULTORIO, EXAME, PROCEDIMENTO, CIRURGIA
    andar            VARCHAR(20),
    data_criacao     DATETIME(6)  NOT NULL,
    data_modificacao DATETIME(6)  NOT NULL,
    CONSTRAINT uk_salas_identificacao UNIQUE (identificacao)
);
