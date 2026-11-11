-- Camada de autenticação: só a equipe da clínica acessa o sistema. Médicos e funcionários têm um
-- usuário (login + perfil); pacientes são apenas registros, sem acesso.
-- Em medicos, usuario_id é NULL apenas para registros criados antes desta migration.
CREATE TABLE usuarios (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    login            VARCHAR(150) NOT NULL,
    senha            VARCHAR(100) NOT NULL,  -- hash BCrypt, nunca a senha em texto puro
    perfil           VARCHAR(20)  NOT NULL,  -- ADMIN, FUNCIONARIO, MEDICO
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    data_criacao     DATETIME(6)  NOT NULL,
    data_modificacao DATETIME(6)  NOT NULL,
    CONSTRAINT uk_usuarios_login UNIQUE (login)
);

CREATE TABLE funcionarios (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(150) NOT NULL,
    cpf              VARCHAR(11)  NOT NULL,
    email            VARCHAR(150),
    telefone         VARCHAR(20),
    data_nascimento  DATE         NOT NULL,
    matricula        VARCHAR(20)  NOT NULL,
    cargo            VARCHAR(100) NOT NULL,
    setor            VARCHAR(100),
    data_admissao    DATE         NOT NULL,
    usuario_id       BIGINT,
    data_criacao     DATETIME(6)  NOT NULL,
    data_modificacao DATETIME(6)  NOT NULL,
    CONSTRAINT uk_funcionarios_cpf UNIQUE (cpf),
    CONSTRAINT uk_funcionarios_matricula UNIQUE (matricula),
    CONSTRAINT uk_funcionarios_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_funcionarios_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

ALTER TABLE medicos ADD COLUMN usuario_id BIGINT;
ALTER TABLE medicos ADD CONSTRAINT uk_medicos_usuario UNIQUE (usuario_id);
ALTER TABLE medicos ADD CONSTRAINT fk_medicos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);
