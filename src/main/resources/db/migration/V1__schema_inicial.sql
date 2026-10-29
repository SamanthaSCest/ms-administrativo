-- ============================================================
-- MS-ADM - schema inicial
-- Todas as tabelas de dados mestres + a tabela de outbox de eventos.
-- Nenhum registro é apagado: "excluir" = ativo = false (RN17).
-- ============================================================

CREATE TABLE especialidade (
  id UUID PRIMARY KEY,
  nome VARCHAR(80) NOT NULL,
  descricao VARCHAR(255),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMPTZ NOT NULL,
  atualizado_em TIMESTAMPTZ NOT NULL,
  criado_por VARCHAR(100),
  atualizado_por VARCHAR(100)
);
-- RN10: nome único sem diferenciar maiúsculas de minúsculas
CREATE UNIQUE INDEX uk_especialidade_nome ON especialidade (LOWER(nome));

CREATE TABLE unidade (
  id UUID PRIMARY KEY,
  nome VARCHAR(100) NOT NULL UNIQUE,
  cnpj CHAR(14) UNIQUE,
  telefone VARCHAR(11),
  cep CHAR(8), logradouro VARCHAR(150), numero VARCHAR(10), complemento VARCHAR(60),
  bairro VARCHAR(80), cidade VARCHAR(80), uf CHAR(2),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMPTZ NOT NULL, atualizado_em TIMESTAMPTZ NOT NULL,
  criado_por VARCHAR(100), atualizado_por VARCHAR(100)
);

CREATE TABLE sala (
  id UUID PRIMARY KEY,
  unidade_id UUID NOT NULL REFERENCES unidade(id),
  identificacao VARCHAR(30) NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  andar VARCHAR(10),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMPTZ NOT NULL, atualizado_em TIMESTAMPTZ NOT NULL,
  criado_por VARCHAR(100), atualizado_por VARCHAR(100),
  CONSTRAINT uk_sala_unidade UNIQUE (unidade_id, identificacao)
);

CREATE TABLE convenio (
  id UUID PRIMARY KEY,
  nome VARCHAR(100) NOT NULL,
  registro_ans CHAR(6) NOT NULL UNIQUE,
  cnpj CHAR(14),
  telefone VARCHAR(11),
  email VARCHAR(150),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMPTZ NOT NULL, atualizado_em TIMESTAMPTZ NOT NULL,
  criado_por VARCHAR(100), atualizado_por VARCHAR(100)
);

CREATE TABLE plano_convenio (
  id UUID PRIMARY KEY,
  convenio_id UUID NOT NULL REFERENCES convenio(id),
  nome VARCHAR(100) NOT NULL,
  codigo VARCHAR(20),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMPTZ NOT NULL, atualizado_em TIMESTAMPTZ NOT NULL,
  criado_por VARCHAR(100), atualizado_por VARCHAR(100),
  CONSTRAINT uk_plano_convenio UNIQUE (convenio_id, nome)
);

CREATE TABLE paciente (
  id UUID PRIMARY KEY,
  nome VARCHAR(150) NOT NULL,
  nome_social VARCHAR(150),
  cpf CHAR(11) NOT NULL UNIQUE,
  data_nascimento DATE NOT NULL,
  sexo VARCHAR(15) NOT NULL,
  email VARCHAR(150),
  telefone_celular VARCHAR(11),
  telefone_fixo VARCHAR(10),
  cep CHAR(8), logradouro VARCHAR(150), numero VARCHAR(10), complemento VARCHAR(60),
  bairro VARCHAR(80), cidade VARCHAR(80), uf CHAR(2),
  resp_nome VARCHAR(150), resp_cpf CHAR(11), resp_parentesco VARCHAR(15), resp_telefone VARCHAR(11),
  consentimento_lgpd_em TIMESTAMPTZ,
  versao_termo_lgpd VARCHAR(20),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMPTZ NOT NULL, atualizado_em TIMESTAMPTZ NOT NULL,
  criado_por VARCHAR(100), atualizado_por VARCHAR(100),
  -- RN04: pelo menos um contato
  CONSTRAINT ck_paciente_contato CHECK (email IS NOT NULL OR telefone_celular IS NOT NULL)
);
CREATE INDEX ix_paciente_nome ON paciente (LOWER(nome));

CREATE TABLE paciente_convenio (
  id UUID PRIMARY KEY,
  paciente_id UUID NOT NULL REFERENCES paciente(id),
  plano_id UUID NOT NULL REFERENCES plano_convenio(id),
  numero_carteirinha VARCHAR(30) NOT NULL,
  validade DATE,
  titular BOOLEAN NOT NULL DEFAULT TRUE,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMPTZ NOT NULL, atualizado_em TIMESTAMPTZ NOT NULL,
  criado_por VARCHAR(100), atualizado_por VARCHAR(100)
);
-- RN05: só um vínculo ATIVO por paciente + plano
CREATE UNIQUE INDEX uk_paciente_plano_ativo ON paciente_convenio (paciente_id, plano_id) WHERE ativo;

CREATE TABLE medico (
  id UUID PRIMARY KEY,
  nome VARCHAR(150) NOT NULL,
  cpf CHAR(11) NOT NULL UNIQUE,
  crm VARCHAR(10) NOT NULL,
  uf_crm CHAR(2) NOT NULL,
  email VARCHAR(150) NOT NULL,
  telefone VARCHAR(11),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em TIMESTAMPTZ NOT NULL, atualizado_em TIMESTAMPTZ NOT NULL,
  criado_por VARCHAR(100), atualizado_por VARCHAR(100),
  -- RN08: CRM + UF único
  CONSTRAINT uk_medico_crm UNIQUE (crm, uf_crm)
);

CREATE TABLE medico_especialidade (
  medico_id UUID NOT NULL REFERENCES medico(id),
  especialidade_id UUID NOT NULL REFERENCES especialidade(id),
  PRIMARY KEY (medico_id, especialidade_id)
);

CREATE TABLE medico_unidade (
  medico_id UUID NOT NULL REFERENCES medico(id),
  unidade_id UUID NOT NULL REFERENCES unidade(id),
  PRIMARY KEY (medico_id, unidade_id)
);

CREATE TABLE medico_convenio (
  medico_id UUID NOT NULL REFERENCES medico(id),
  convenio_id UUID NOT NULL REFERENCES convenio(id),
  PRIMARY KEY (medico_id, convenio_id)
);

-- Outbox: o evento é gravado na mesma transação do cadastro (RN19)
-- e um agendador publica no RabbitMQ depois (etapa 7).
CREATE TABLE outbox_evento (
  id UUID PRIMARY KEY,
  tipo VARCHAR(60) NOT NULL,
  agregado_id UUID NOT NULL,
  payload JSONB NOT NULL,
  criado_em TIMESTAMPTZ NOT NULL,
  publicado_em TIMESTAMPTZ
);
CREATE INDEX ix_outbox_pendente ON outbox_evento (criado_em) WHERE publicado_em IS NULL;