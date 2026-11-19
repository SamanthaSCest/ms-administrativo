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
| API de integração | `controllers/IntegracaoController`, `controllers/PacienteIntegracaoController`, `dto/integracao/` | Leituras para outros microserviços (ver seção "Integração e eventos") |
| Máscara de CPF (LGPD) | `utils/CpfMascara` | `12345678909` → `***.456.789-**` |
| Validadores de CPF e CNPJ | `validation/@Cpf`, `validation/@Cnpj` | Conferem os dígitos verificadores (prontos para usar nos DTOs) |
| Endereço e UF | `entities/Endereco`, `entities/UF` | Objeto embutido de endereço, para Unidade e Paciente |
| Outbox de eventos + RabbitMQ | `entities/OutboxEvento`, `services/OutboxService`, `services/PublicadorOutbox`, `config/RabbitMqEventosConfiguration`, `db/migration/V2__criar_outbox_evento.sql` | Cada cadastro publica seus eventos pelo `services/EventoService`, na mesma transação; a cada 5 s o publicador envia os pendentes para a exchange `adm.events` (topic) |

O RabbitMQ sobe junto no `docker compose` (painel em http://localhost:15672, usuário `clinical`/`clinical`).
Nos testes o publicador fica desligado (`outbox.publicador.habilitado: false`).

## Autenticação e cadastro de funcionários

> "Como administrador, quero cadastrar funcionários com seus dados pessoais e profissionais, para manter a equipe registrada no sistema."

Login, senha, grupos e permissões ficam no **Keycloak** (realm `clinical`, definido em [`keycloak/realm-clinical.json`](../keycloak/realm-clinical.json)). Este serviço valida os tokens e mantém o usuário do Keycloak em dia com o cadastro de médicos e funcionários.

### Modelagem

```text
Usuario (usuarios)            Pessoa (abstrata: nome, cpf, email, telefone, dataNascimento)
  id, login (CPF, único)        ├── Medico      (medicos)       + crm/ufCrm, especialidades,   -> grupo MEDICO
  keycloak_id (único), ativo    ├── Funcionario (funcionarios)  + matricula, cargo, setor,     -> grupo do cargo
        ^                       │                                 dataAdmissao, usuario
        │                       └── Paciente    (pacientes)     sem usuário: não acessa o sistema
        └──── 1:1 (usuario_id, único) em medicos e funcionarios

Keycloak (realm clinical): usuário (username = CPF, senha, enabled) ── grupos ── client roles (= enum Permissao)
```

- Só a **equipe** acessa o sistema. Médicos e funcionários implementam `PossuiUsuario` e têm **um** usuário. O login é o **CPF**; a senha vai no próprio cadastro (`senha`, obrigatória no POST) e segue direto para o Keycloak, sem ser guardada aqui. Política de senha do realm: de 8 a 72 caracteres e diferente do login (CPF); senha recusada pelo Keycloak volta como 422 com a mensagem dele.
- **Paciente não tem login**: é apenas um registro, e o cadastro não pede senha.
- Cadastrar cria o usuário no Keycloak (Admin API, `identidade/KeycloakProvedorIdentidade`). Atualizar sincroniza login, nome, e-mail e, se vier, a senha. Inativar ou desativar desliga o usuário no Keycloak. Remover apaga o usuário lá depois do commit.
- O Keycloak não participa da transação do banco: se a gravação falhar depois de criar o usuário no Keycloak, ele é apagado lá (compensação em `UsuarioService`). Se o Keycloak estiver fora do ar, a operação devolve 503 e nada é gravado.
- Um mesmo CPF só pode ter um usuário: cadastrar um médico com o CPF de um funcionário retorna 409. Um funcionário pode ter o mesmo CPF de um paciente.
- O **administrador inicial** (usuário `admin`, senha em `KEYCLOAK_CLINICAL_ADMIN_PASSWORD` do `.env`) vem do realm, sem pessoa vinculada. Outros admins são funcionários com `"cargo": "ADMINISTRADOR"`.
- Migration `V15__keycloak_usuarios.sql`: remove as tabelas de grupos e permissões e a senha local. Usuários criados antes do Keycloak ficam sem `keycloak_id` e são recriados lá quando o cadastro recebe uma senha num PUT (a senha antiga não migra).

### Endpoints

| Método e rota | Quem | Descrição |
|---|---|---|
| `GET /auth/me` | autenticado | Login, nome, grupos e permissões, lidos do token |
| `GET /usuarios`, `GET /usuarios/{id}` | `USUARIO_GERENCIAR` | Consulta usuários (o detalhe traz os grupos do Keycloak) |
| `PATCH /usuarios/{id}/status` | `USUARIO_GERENCIAR` | `{ativo}`: bloqueia/libera o login no Keycloak (não pode desativar a si mesmo) |
| `GET /grupos` | `USUARIO_GERENCIAR` | Nomes dos grupos do Keycloak, para o campo `grupos` do funcionário |
| `/funcionarios` (CRUD) | `FUNCIONARIO_CONSULTAR` / `_GERENCIAR` | Cadastro de funcionários + usuário. `grupos` (opcional, nomes) escolhe grupos além do do cargo e exige também `USUARIO_GERENCIAR` (403 `ADM-FUN-001`) |

Login, logout e troca de senha são feitos no Keycloak (tela de login ou endpoint de token).

### Controle de acesso (RBAC)

- O acesso é por **permissão** (`entities/Permissao`): cada uma é uma *client role* do client `ms-administrativo` no Keycloak. `<RECURSO>_CONSULTAR` libera as leituras (GET e as buscas por POST) e `<RECURSO>_GERENCIAR` libera a escrita.
- Os usuários recebem as roles pelos **grupos do Keycloak**. Há um grupo por cargo e um para médicos; quem é cadastrado entra no grupo do cargo, e mudar o cargo troca esse grupo sem mexer nos demais:
  - `ADMINISTRADOR`: todas as permissões.
  - `RECEPCIONISTA` (atendimento e agendamento): gerencia pacientes; consulta médicos, especialidades, convênios, salas, procedimentos, custos e disponibilidades.
  - `ESTOQUISTA`: gerencia insumos; consulta procedimentos e salas.
  - `RESPONSAVEL_FINANCEIRO`: gerencia custos dos médicos; consulta convênios, procedimentos, insumos, médicos, especialidades e pacientes.
  - `MEDICO`: consulta pacientes, médicos, especialidades, convênios, salas, procedimentos, insumos e disponibilidades.
- Criar grupos, mudar as roles de um grupo ou dar uma role avulsa a alguém é feito no console do Keycloak (http://localhost:8180, com `KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD` do `.env`). Para valer em outros ambientes, atualize também o `realm-clinical.json`. Uma permissão **nova** precisa existir no enum `Permissao` e no realm (o `RbacApiTest` confere os dois).
- A matriz completa está na descrição do contrato (`static/contract/openapi-administrativo.yaml`) e as regras por rota em `config/SecurityConfig`.

### Como funciona

- O Spring Security Resource Server valida o token pelas chaves públicas do realm (JWKS), pela expiração e pelo emissor (`iss`). As roles de `resource_access.ms-administrativo.roles` viram authorities (`config/KeycloakRolesConverter`), e o `preferred_username` vira o nome do usuário (usado na auditoria). Como as roles vão no token, **mudanças de acesso valem a partir do próximo token** (no máximo 15 min).
- Erros de segurança (401/403) saem no mesmo formato `ErrorResponse` (`config/SegurancaErrorHandler`).
- Serviço-a-serviço: o ms-agendamento usa **client credentials** com o client `ms-agendamento`; a service account dele tem leitura dos cadastros, `/integracao` e escrita em `/insumos`. O provedor de token (`common/security/ClientCredentialsTokenProvider`) guarda o token até perto de expirar.
- Swagger UI: http://localhost:8081/swagger-ui.html → botão **Authorize** → `keycloak` (login no Keycloak). Também dá para colar um token em `bearerAuth`:

```bash
set -a; . ./.env; set +a     # na raiz do repositório: carrega as senhas do .env
curl -s -d grant_type=password -d client_id=clinical-web -d username=admin \
  -d password="$KEYCLOAK_CLINICAL_ADMIN_PASSWORD" \
  http://localhost:8180/realms/clinical/protocol/openid-connect/token
```

| Variável | Padrão (só desenvolvimento) | Uso |
|---|---|---|
| `KEYCLOAK_URL` | `http://localhost:8180` | Onde este serviço busca as chaves e chama a Admin API (no compose, `http://keycloak:8080`) |
| `KEYCLOAK_ISSUER` | `http://localhost:8180/realms/clinical` | `iss` esperado no token (endereço **público** do Keycloak) |
| `KEYCLOAK_REALM` | `clinical` | Realm |
| `KEYCLOAK_CLIENT_ID` | `ms-administrativo` | Client das roles e da service account da Admin API |
| `KEYCLOAK_CLIENT_SECRET` | sem padrão (fora do Docker, `KEYCLOAK_ADMINISTRATIVO_SECRET` do `.env`) | Segredo desse client |
| `DB_USER` / `DB_PASSWORD`, `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | sem padrão (vêm do `.env`) | Banco e RabbitMQ; ver "Credenciais" no README da raiz |

Nos testes não há Keycloak: `support/IdentidadeEmMemoria` substitui a Admin API e o token vem pronto. `@ComGrupo("RECEPCIONISTA")` (classe ou método) e `.with(Acessos.grupo("dr", "MEDICO"))` (por requisição) montam um token com as roles reais do grupo, lidas do `realm-clinical.json`. `"SERVICO"` simula a service account do ms-agendamento. A sincronização com o Keycloak está em `AuthApiIntegrationTest`, o RBAC em `RbacApiTest` e as chamadas HTTP à Admin API em `KeycloakProvedorIdentidadeTest`.

## Entidades do diagrama de classes

| Entidade | Tabela(s) | Endpoints | Regras principais |
|---|---|---|---|
| `Paciente` (+ `Endereco` embutido) | `pacientes` | `/pacientes` | `sexo`, `telefone` (celular), `telefoneFixo`, `endereco`; `calcularIdade()`/`ehMenorDeIdade()`; **menor de idade exige responsável** (422) |
| `ResponsavelLegal` | `responsaveis_legais` | `/responsaveis` | CPF único e válido; não remove com pacientes vinculados |
| `Medico` | `medicos`, `medico_especialidades` | `/medicos` | `ufCrm`; **várias especialidades** (`especialidadeIds`); CRM único por UF |
| `Funcionario` | `funcionarios` | `/funcionarios` | `cargo` (enum `Cargo`) define o grupo de acesso padrão |
| `Convenio` | `convenios` | `/convenios` | registro ANS e CNPJ únicos (CNPJ validado) |
| `Sala` | `salas` | `/salas` | `numero` único, `tipo` (CONSULTORIO, CIRURGIA ou PROCEDIMENTO), `andar` e `observacoes`; não remove com disponibilidades |
| `Insumo` | `insumos` | `/insumos`, `PATCH /insumos/{id}/custo` | código único; **outros microserviços podem escrever** (client credentials) |
| `Procedimento` + `InsumoProcedimento` | `procedimentos`, `insumos_procedimento` | `/procedimentos` | código TUSS único; insumos sem repetição; resposta traz `custoInsumos` |
| `CustoMedico` → `CustoMedicoConsulta` / `CustoMedicoProcedimento` | `custos_medicos` (SINGLE_TABLE) | `/custos-medicos`, `/custos-medicos/vigente` | médico precisa atender a especialidade; vigências não se sobrepõem (409) |
| `DisponibilidadeMedica` → `DisponibilidadeDiaria` / `DisponibilidadeExcecao` | `disponibilidades_medicas` (SINGLE_TABLE) | `/disponibilidades`, `/disponibilidades/agenda` | cadastrada pelo ADMIN; sem sobreposição do médico nem da sala (409); a agenda do dia aplica bloqueios e horários extras |

Enums: `Sexo`, `Cargo`, `TipoSala`, `DiaSemana`, `UF`. Custos e disponibilidades são **composição** do médico: removidos junto com ele (`ON DELETE CASCADE`). Migrations V4 a V9.

### EntidadeBase (uuid, ativo e auditoria)

Todas as entidades acima estendem `entities/EntidadeBase`, que acrescenta à `BaseEntity` do `common` (id + datas):

| Campo | O que é |
|---|---|
| `uuid` | Identificador público adicional, único, gerado na criação. **A chave continua sendo o `id` (BIGINT)** |
| `ativo` | `inativar()` / `reativar()` via `PATCH /{recurso}/{id}/status` com `{"ativo": false}` |
| `criadoPor` / `atualizadoPor` | Login de quem criou/alterou (Spring Data Auditing, `config/AuditoriaConfig`): o `preferred_username` do token (CPF, `admin` ou `service-account-<client>` para outro microserviço) ou `sistema` sem usuário |

Por que o `id` não virou UUID: o ms-agendamento referencia médicos e pacientes como número em todas as camadas (`medico_id BIGINT` na tabela, `Long medicoId` na entidade, no DTO e no cliente HTTP). Trocar a chave quebraria a integração; o `uuid` como coluna extra entrega o identificador do diagrama sem esse risco. Pelo mesmo motivo a `EntidadeBase` fica neste módulo, e não no `common` (o `Agendamento` também estende a `BaseEntity`). As datas mantêm os nomes `dataCriacao`/`dataModificacao` da `BaseEntity` (no diagrama: `criadoEm`/`atualizadoEm`).

Regras de inativação:
- Inativar **não apaga**: o registro continua consultável. O `DELETE` continua apagando de verdade.
- Cadastro inativo não entra em vínculo **novo** (médico/sala/procedimento/especialidade/insumo/responsável → 422); vínculos já existentes são preservados.
- Custos e disponibilidades inativos são ignorados (vigência, conflitos e agenda); médico inativo tem agenda vazia.
- Inativar médico ou funcionário bloqueia o login; ninguém inativa o próprio cadastro (422).
- `GET /integracao/pacientes/{id}/situacao` devolve `ativo` real do paciente.
- Quem pode inativar segue a regra de escrita de cada recurso (ex.: pacientes → ADMIN/FUNCIONARIO; médicos → ADMIN).

Migration V10: adiciona as colunas às 12 tabelas e gera um UUID (`UUID()` do banco) para os registros existentes.

## Pacientes: busca, LGPD e convênios

- `GET /pacientes[?ativo=]` sem `ativo` traz só os ativos. Filtro por nome e CPF fica em `POST /pacientes/busca` (corpo `{nome, cpf, ativo}`), para não pôr dado pessoal na URL (RNF08). As listagens devolvem o CPF mascarado; o CPF completo só sai no `GET /pacientes/{id}`.
- `GET /pacientes/{id}` é o detalhe completo: inclui `convenios`, os vínculos ativos com carteirinha, validade e `vigente`. As listagens não trazem esse campo.
- Consentimento LGPD (RF26): `consentimentoLgpd` + `versaoTermoLgpd` (obrigatória quando há consentimento). O servidor registra `dataConsentimentoLgpd`, que só muda quando a versão do termo muda; retirar o consentimento limpa os dois.
- Vínculo com convênio (RF06, HU04) em `/pacientes/{id}/convenios`, direto com o convênio (o plano é de outro grupo). Regras: convênio ativo (`ADM-PCV-001`), um vínculo ativo por convênio (`ADM-PCV-002`, 409), carteirinha não vencida (`ADM-PCV-003`), paciente ativo (`ADM-PCV-004`) e vínculo desvinculado não muda mais (`ADM-PCV-005`). `DELETE` desvincula (inativa) e mantém o histórico. Inativar o convênio não mexe no vínculo, mas ele deixa de ser vigente (RN16).
- Especialidade com médico ativo não pode ser inativada (RN11): 422 `ADM-ESP-002` com `medicosVinculados`.

## Funcionários, médicos e convênios: listagem e busca

- `GET /funcionarios`, `GET /medicos` e `GET /convenios` aceitam `?ativo=`; sem ele trazem só os ativos. Em convênios, os ativos são os disponíveis para novas vinculações; `?ativo=false` traz os inativos para reativação.
- `POST /funcionarios/busca` (`FUNCIONARIO_CONSULTAR`), corpo `{nome, cpf, ativo}`.
- `POST /medicos/busca` (`MEDICO_CONSULTAR`, como o `GET`), corpo `{nome, crm, ufCrm, especialidadeId, ativo}`; os filtros se combinam.
- Como em pacientes, os filtros vão no corpo (RNF08), nenhum resultado devolve página vazia, e as listagens e buscas de funcionários e médicos devolvem o CPF mascarado (RN20); o CPF completo só sai no `GET /{id}`.

## Integração e eventos

Escopo do grupo: só o ms-administrativo (os demais microserviços são de outros grupos). Não há Unidade, e Plano de convênio fica com outro grupo.

### API de integração (somente leitura, `INTEGRACAO_CONSULTAR`: service accounts e ADMINISTRADOR)

| Rota | O que devolve |
|---|---|
| `GET /integracao/especialidades` | Especialidades ativas |
| `GET /integracao/medicos/{id}` | `MedicoResumo` (sem CPF, com especialidades) |
| `GET /integracao/medicos/{id}/situacao` | `{existe, ativo}`, sempre 200 |
| `GET /integracao/medicos/{id}/atende?especialidadeId=` | `{atende}`: false se o médico não existe, está inativo ou não atende |
| `GET /integracao/medicos?ids=1,2&especialidadeId=` | Lote (até 100 ids) e/ou médicos ativos da especialidade |
| `GET /integracao/pacientes/{id}`, `/situacao`, `/contato` | Resumo (CPF mascarado), situação e contato do paciente |
| `GET /integracao/pacientes?ids=1,2` | Pacientes em lote (até 100), CPF mascarado |
| `GET /integracao/pacientes/{id}/convenios/vigentes[?data=]` | Vínculos vigentes na data (hoje, se omitida): vínculo ativo, convênio ativo e carteirinha válida |
| `GET /integracao/salas[?tipo=]`, `/integracao/salas/{id}` | Salas ativas (por tipo) e resumo da sala |
| `GET /integracao/convenios`, `/integracao/convenios/{id}` | Convênios ativos e resumo do convênio |

Outras leituras úteis para os outros grupos (também liberadas para a service account do ms-agendamento): `GET /disponibilidades/agenda?medicoId=&data=`, `GET /custos-medicos/vigente`, `GET /insumos` (o ms-estoque também escreve em `/insumos`).

### Eventos publicados (exchange `adm.events`, tipo topic, routing key = tipo do evento)

Cada cadastro publica, na mesma transação da alteração (`services/EventoService` → outbox → RabbitMQ):

| Agregado | Tipos |
|---|---|
| especialidade, sala, disponibilidade | `.criada`, `.atualizada`, `.inativada`, `.reativada`, `.removida` |
| paciente, medico, funcionario, convenio, responsavel-legal, insumo, procedimento, custo-medico | `.criado`, `.atualizado`, `.inativado`, `.reativado`, `.removido` |
| vínculo paciente–convênio (agregadoId = id do paciente) | `paciente.convenio-vinculado`, `paciente.convenio-atualizado`, `paciente.convenio-desvinculado` |

Envelope: `eventoId`, `tipo`, `versao`, `ocorridoEm`, `origem` (`ms-administrativo`), `agregadoId`, `camposAlterados` (nos `.atualizado`) e `dados` (o Resumo da entidade, o mesmo da API de integração, sem CPF completo).
Um PUT que não muda nada e um PATCH de status repetido não publicam evento. Remover um médico apaga em cascata seus custos e disponibilidades; o evento `medico.removido` representa os três.

Exemplo para assinar: fila ligada à exchange `adm.events` com routing key `paciente.*` (todos os eventos de paciente) ou `#` (todos).
