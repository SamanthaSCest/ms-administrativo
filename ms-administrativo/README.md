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
  id, login (único)             ├── Medico      (medicos)       + crm, especialidade, usuario  -> perfil MEDICO
  senha (hash BCrypt)           ├── Funcionario (funcionarios)  + matricula, cargo, setor,     -> perfil ADMIN ou FUNCIONARIO
  perfil, ativo                 │                                 dataAdmissao, usuario
        ^                       └── Paciente    (pacientes)     sem usuário: não acessa o sistema
        └──── 1:1 (usuario_id, único) em medicos e funcionarios
```

- Só a **equipe** acessa o sistema. Médicos e funcionários implementam `PossuiUsuario` e têm **um** usuário (`@OneToOne`, cascata). O login é o **CPF**; a senha vai no próprio cadastro (`senha`, obrigatória no POST).
- **Paciente não tem login**: é apenas um registro, e o cadastro não pede senha.
- Remover o médico ou o funcionário remove o usuário. Mudar o CPF muda o login.
- Um mesmo CPF só pode ter um usuário: cadastrar um médico com o CPF de um funcionário retorna 409. Um funcionário pode ter o mesmo CPF de um paciente.
- O **administrador inicial** (sem pessoa vinculada) é criado na inicialização por `config/AdminInicializador`, a partir de `ADMIN_LOGIN`/`ADMIN_PASSWORD` (padrão local `admin`/`admin12345`). Outros admins são funcionários com `"perfil": "ADMIN"`.
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
- Serviço-a-serviço: o ms-agendamento envia `X-Api-Key` (`config/ApiKeyAuthenticationFilter`) e recebe o perfil `SERVICO` (somente leitura).
- Swagger UI: http://localhost:8081/swagger-ui.html → `POST /auth/login` → botão **Authorize** → cole o `accessToken`.

| Variável | Padrão (só desenvolvimento) | Uso |
|---|---|---|
| `JWT_SECRET` | `dev-only-troque-...` | Segredo HMAC, mínimo 32 bytes |
| `JWT_EXPIRACAO` | `2h` | Validade do token |
| `ADMIN_LOGIN` / `ADMIN_PASSWORD` | `admin` / `admin12345` | Administrador inicial |
| `INTEGRACAO_API_KEY` | `dev-integracao-key` | Chave `X-Api-Key` (no ms-agendamento: `ADMINISTRATIVO_API_KEY`) |

Nos testes, `AdministrativoApiIntegrationTest` roda como ADMIN via `@WithMockUser`; o fluxo real (login, token, perfis, API key) está em `AuthApiIntegrationTest`.
