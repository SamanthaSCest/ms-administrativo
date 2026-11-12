# Módulo administrativo

Este módulo faz parte do projeto `clinicalapi` e segue o pacote base:

`br.edu.imepac.clinicalapi.administrativo`

## Estrutura principal

```text
src/
  main/
    java/
      br/
        edu/
          imepac/
            clinicalapi/
              administrativo/
                Application.java
                entities/
                repositories/
                services/
                controllers/
                utils/
                dto/
                mapper/
                exceptions/
                config/
                validation/
```

## Convenções

- `entities`: entidades de domínio / JPA
- `repositories`: interfaces de persistência
- `services`: regras de negócio
- `controllers`: endpoints REST
- `utils`: utilitários e helpers
- `dto`: request/response da API
- `mapper`: conversão entre DTOs e entidades
- `exceptions`: exceções e tratamento de erros
- `config`: configurações do Spring e beans
- `validation`: validações e regras específicas

## Observações

- Esse README define a base inicial da organização do módulo.
- Novos pacotes podem ser adicionados conforme a funcionalidade evoluir.
- A imagem Docker compila e executa o serviço com Eclipse Temurin 25 LTS.

## Extensões do grupo (MS-ADM)

Além dos CRUDs de Especialidade, Médico e Paciente do modelo, este módulo traz:

| O quê | Onde | Para que serve |
|---|---|---|
| API de integração do paciente | `controllers/PacienteIntegracaoController`, `services/PacienteIntegracaoService`, `dto/integracao/` | Leituras para outros microserviços: `GET /integracao/pacientes/{id}` (CPF mascarado), `/situacao` (sempre 200, `existe` true/false) e `/contato` |
| Máscara de CPF (LGPD) | `utils/CpfMascara` | `12345678909` → `***.456.789-**` |
| Validadores de CPF e CNPJ | `validation/@Cpf`, `validation/@Cnpj` | Conferem os dígitos verificadores (prontos para usar nos DTOs) |
| Endereço e UF | `entities/Endereco`, `entities/UF` | Objeto embutido de endereço, para Unidade e Paciente |
| Outbox de eventos + RabbitMQ | `entities/OutboxEvento`, `services/OutboxService`, `services/PublicadorOutbox`, `config/RabbitMqEventosConfiguration`, `db/migration/V2__criar_outbox_evento.sql` | O serviço chama `outboxService.registrar(...)` dentro da transação; a cada 5 s o publicador envia os pendentes para a exchange `adm.events` (topic) |

O RabbitMQ sobe junto no `docker compose` (painel em http://localhost:15672, usuário `clinical`/`clinical`).
Nos testes o publicador fica desligado (`outbox.publicador.habilitado: false`).

## Autenticação e cadastro de funcionários

> "Como administrador, quero cadastrar funcionários com seus dados pessoais e profissionais, para manter a equipe registrada no sistema."

### Modelagem

```text
Usuario (usuarios)            Pessoa (abstrata: nome, cpf, email, telefone, dataNascimento)
  id, login (único)             ├── Medico      (medicos)       + crm/ufCrm, especialidades,   -> perfil MEDICO
  senha (hash BCrypt)           ├── Funcionario (funcionarios)  + matricula, cargo, setor,     -> perfil pelo cargo
  perfil, ativo                 │                                 dataAdmissao, usuario
        ^                       └── Paciente    (pacientes)     sem usuário: não acessa o sistema
        └──── 1:1 (usuario_id, único) em medicos e funcionarios
```

- Só a **equipe** acessa o sistema. Médicos e funcionários implementam `PossuiUsuario` e têm **um** usuário (`@OneToOne`, cascata). O login é o **CPF**; a senha vai no próprio cadastro (`senha`, obrigatória no POST).
- **Paciente não tem login**: é apenas um registro, e o cadastro não pede senha.
- Remover o médico ou o funcionário remove o usuário. Mudar o CPF muda o login.
- Um mesmo CPF só pode ter um usuário: cadastrar um médico com o CPF de um funcionário retorna 409. Um funcionário pode ter o mesmo CPF de um paciente.
- O **administrador inicial** (sem pessoa vinculada) é criado na inicialização por `config/AdminInicializador`, a partir de `ADMIN_LOGIN`/`ADMIN_PASSWORD` (padrão local `admin`/`admin12345`). Outros admins são funcionários com `"cargo": "ADMINISTRADOR"` (o cargo define o perfil: ADMINISTRADOR → `ADMIN`; demais → `FUNCIONARIO`).
- Migration: `db/migration/V3__criar_usuarios_e_funcionarios.sql` (médicos antigos ficam com `usuario_id` nulo até receberem senha num PUT).

### Endpoints

| Método e rota | Quem | Descrição |
|---|---|---|
| `POST /auth/login` | público | `{login, senha}` → `{accessToken, tokenType, expiresIn, usuario}` |
| `GET /auth/me` | autenticado | Dados do usuário do token |
| `PUT /auth/senha` | autenticado | `{senhaAtual, novaSenha}` |
| `GET /usuarios`, `GET /usuarios/{id}` | ADMIN | Consulta usuários |
| `PATCH /usuarios/{id}/status` | ADMIN | `{ativo}`: bloqueia/libera o login (não pode desativar a si mesmo) |
| `/funcionarios` (CRUD) | ADMIN | Cadastro de funcionários + usuário |

A matriz completa de permissões está na descrição do contrato (`static/contract/openapi-administrativo.yaml`) e em `config/SecurityConfig`.

### Como funciona

- JWT **HS256** emitido pelo próprio serviço (`services/TokenService`) e validado pelo Spring Security Resource Server (`config/SecurityConfig`). O claim `perfil` vira `ROLE_<perfil>`.
- Erros de segurança (401/403) saem no mesmo formato `ErrorResponse` (`config/SegurancaErrorHandler`).
- Serviço-a-serviço: o ms-agendamento e o ms-estoque enviam `X-Api-Key` (`config/ApiKeyAuthenticationFilter`) e recebem o perfil `SERVICO` (leitura; escrita só em `/insumos`, para o estoque).
- Swagger UI: http://localhost:8081/swagger-ui.html → `POST /auth/login` → botão **Authorize** → cole o `accessToken`.

| Variável | Padrão (só desenvolvimento) | Uso |
|---|---|---|
| `JWT_SECRET` | `dev-only-troque-...` | Segredo HMAC, mínimo 32 bytes |
| `JWT_EXPIRACAO` | `2h` | Validade do token |
| `ADMIN_LOGIN` / `ADMIN_PASSWORD` | `admin` / `admin12345` | Administrador inicial |
| `INTEGRACAO_API_KEY` | `dev-integracao-key` | Chave `X-Api-Key` (no ms-agendamento: `ADMINISTRATIVO_API_KEY`) |

Nos testes, `AdministrativoApiIntegrationTest` roda como ADMIN via `@WithMockUser`; o fluxo real (login, token, perfis, API key) está em `AuthApiIntegrationTest`.

## Entidades do diagrama de classes

| Entidade | Tabela(s) | Endpoints | Regras principais |
|---|---|---|---|
| `Paciente` (+ `Endereco` embutido) | `pacientes` | `/pacientes` | `sexo`, `telefone` (celular), `telefoneFixo`, `endereco`; `calcularIdade()`/`ehMenorDeIdade()`; **menor de idade exige responsável** (422) |
| `ResponsavelLegal` | `responsaveis_legais` | `/responsaveis` | CPF único e válido; não remove com pacientes vinculados |
| `Medico` | `medicos`, `medico_especialidades` | `/medicos` | `ufCrm`; **várias especialidades** (`especialidadeIds`); CRM único por UF |
| `Funcionario` | `funcionarios` | `/funcionarios` | `cargo` (enum `Cargo`) define o perfil de acesso |
| `Convenio` | `convenios` | `/convenios` | registro ANS e CNPJ únicos (CNPJ validado) |
| `Sala` | `salas` | `/salas` | identificação única; não remove com disponibilidades |
| `Insumo` | `insumos` | `/insumos`, `PATCH /insumos/{id}/custo` | código único; **ms-estoque pode escrever** (X-Api-Key) |
| `Procedimento` + `InsumoProcedimento` | `procedimentos`, `insumos_procedimento` | `/procedimentos` | código TUSS único; insumos sem repetição; resposta traz `custoInsumos` |
| `CustoMedico` → `CustoMedicoConsulta` / `CustoMedicoProcedimento` | `custos_medicos` (SINGLE_TABLE) | `/custos-medicos`, `/custos-medicos/vigente` | médico precisa atender a especialidade; vigências não se sobrepõem (409) |
| `DisponibilidadeMedica` → `DisponibilidadeDiaria` / `DisponibilidadeExcecao` | `disponibilidades_medicas` (SINGLE_TABLE) | `/disponibilidades`, `/disponibilidades/agenda` | cadastrada pelo ADMIN; sem sobreposição do médico nem da sala (409); a agenda do dia aplica bloqueios e horários extras |

Enums: `Sexo`, `Cargo`, `TipoSala`, `DiaSemana`, `UF`. Custos e disponibilidades são **composição** do médico: removidos junto com ele (`ON DELETE CASCADE`). Migrations V4 a V9.

**Ainda não implementado (decisão do time):** a `EntidadeBase` do diagrama com `UUID id`, `ativo` (`inativar()`/`reativar()`) e auditoria (`criadoPor`/`atualizadoPor`). Trocar `Long` por `UUID` afeta todas as tabelas e o ms-agendamento, que guarda `medicoId`/`pacienteId` como número; por isso ficou para uma etapa própria, coordenada entre os serviços.
