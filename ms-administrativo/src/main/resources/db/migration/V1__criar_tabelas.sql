-- Migration Flyway: executada uma única vez, em ordem de versão (V1, V2...).
-- NUNCA edite uma migration já aplicada; crie uma nova (V2__...).
CREATE TABLE especialidades (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(100) NOT NULL,
    descricao        VARCHAR(255),
    data_criacao     DATETIME(6)  NOT NULL,
    data_modificacao DATETIME(6)  NOT NULL,
    CONSTRAINT uk_especialidades_nome UNIQUE (nome)
);

CREATE TABLE medicos (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(150) NOT NULL,
    cpf              VARCHAR(11)  NOT NULL,
    email            VARCHAR(150),
    telefone         VARCHAR(20),
    data_nascimento  DATE         NOT NULL,
    crm              VARCHAR(20)  NOT NULL,
    especialidade_id BIGINT       NOT NULL,
    data_criacao     DATETIME(6)  NOT NULL,
    data_modificacao DATETIME(6)  NOT NULL,
    CONSTRAINT uk_medicos_cpf UNIQUE (cpf),
    CONSTRAINT uk_medicos_crm UNIQUE (crm),
    CONSTRAINT fk_medicos_especialidade FOREIGN KEY (especialidade_id) REFERENCES especialidades (id)
);

CREATE TABLE pacientes (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(150) NOT NULL,
    cpf              VARCHAR(11)  NOT NULL,
    email            VARCHAR(150),
    telefone         VARCHAR(20),
    data_nascimento  DATE         NOT NULL,
    data_criacao     DATETIME(6)  NOT NULL,
    data_modificacao DATETIME(6)  NOT NULL,
    CONSTRAINT uk_pacientes_cpf UNIQUE (cpf)
);
