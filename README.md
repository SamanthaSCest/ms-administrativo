# Clinical API (IMEPAC)

Backend da clínica médica em **microserviços**. Este repositório traz dois serviços completos
(`ms-administrativo` e `ms-agendamento`) e um `api-gateway`, para servirem de **modelo** aos demais módulos.

```
                        ┌──────────────────┐
  cliente ─ HTTP/JSON ─▶│   api-gateway    │  :8080   (Spring Cloud Gateway)
                        └───────┬──────────┘
              /administrativo/**│        │/agendamento/**
                       ┌────────▼───┐  ┌─▼───────────┐
                       │ms-administ.│◀─│ms-agendament│  agendamento chama o administrativo (REST)
                       │   :8081    │  │    :8082    │  para validar médico e paciente
                       └─────┬──────┘  └──────┬──────┘
                        MySQL próprio (:3307)    MySQL próprio (:3308)
                        clinical_administrativo  clinical_agendamento
```

| Módulo | Porta | Responsabilidade |
|---|---|---|
| `common` | – | Biblioteca: exceções, `ErrorResponse`, `BaseEntity`, `BaseExceptionHandler` (handlers de erro reaproveitados pelos módulos) |
| `ms-administrativo` | 8081 | Especialidades, Médicos e Pacientes |
| `ms-agendamento` | 8082 | Consultas (agendar, remarcar, cancelar, realizar) |
| `api-gateway` | 8080 | Ponto de entrada único; roteia por prefixo |

(Segurança/Keycloak, AWS e Kubernetes ficam para uma etapa futura.)

## Tecnologias e para que servem

| Tecnologia | Onde | Para que serve |
|---|---|---|
| **Java 17** | todos | Linguagem/plataforma (LTS). |
| **Maven** (multi-módulo) | raiz | Compila, roda os testes e empacota. O `pom.xml` raiz declara os módulos e as versões das bibliotecas; cada módulo só lista o que usa. |
| **Spring Boot 3.3** | todos | Sobe a aplicação com configuração automática e servidor web embutido (Tomcat/Netty); empacota tudo num único `.jar` executável. |
| **Spring Web (MVC)** | serviços | Cria os endpoints REST (`@RestController`, `@GetMapping`...). |
| **Spring Data JPA + Hibernate** | serviços | Mapeia classes Java (entidades) para tabelas e gera o SQL; os repositories são só interfaces. |
| **MySQL 8** | docker-compose | Banco relacional. **Um banco por microserviço**: cada serviço é dono dos seus dados e ninguém acessa o banco do outro (só pela API). |
| **Flyway** | serviços | Versiona o schema do banco com scripts SQL (`db/migration/V1__...sql`) executados automaticamente na subida. O Hibernate só *valida* (`ddl-auto: validate`), nunca altera tabelas. |
| **Bean Validation (Jakarta)** | serviços | Valida os DTOs com anotações (`@NotBlank`, `@Past`, `@Future`...) quando o controller usa `@Valid`. |
| **ModelMapper** | serviços | Converte DTO <-> entidade automaticamente, evitando código repetitivo de getters/setters. |
| **Lombok** | todos | Gera getters/setters/construtores em tempo de compilação (`@Getter`, `@Setter`, `@RequiredArgsConstructor`). |
| **SLF4J + Logback** | todos | SLF4J é a API de logs usada no código (`LoggerFactory.getLogger`); o Logback é quem escreve, já incluído no Spring Boot. |
| **@RestControllerAdvice / @ExceptionHandler** | `exceptions/GlobalExceptionHandler` de cada serviço (herda `BaseExceptionHandler` do `common`) | Ponto único de tratamento de erros (Exception Handling no Spring MVC): converte exceções no JSON padrão (`ErrorResponse`) com o HTTP status correto. |
| **RestClient** | `ms-agendamento` | Cliente HTTP do Spring para um microserviço chamar outro (aqui, agendamento -> administrativo), com timeouts. |
| **Spring Cloud Gateway** | `api-gateway` | Porta de entrada única: recebe as requisições e roteia por prefixo (`/administrativo/**`, `/agendamento/**`), sem regra de negócio. Nas rotas futuras é onde entrariam autenticação e CORS. |
| **Spring Boot Actuator** | todos | Endpoints de saúde (`/actuator/health`) usados pelos healthchecks do Docker. |
| **OpenAPI / Swagger** | serviços | O contrato da API (YAML) é a especificação combinada entre os times. |
| **springdoc-openapi (Swagger UI)** | serviços | Página web interativa (`/swagger-ui.html`) que exibe o contrato e permite testar os endpoints. |
| **JUnit 5** | testes | Framework de testes (`@Test`, asserções). |
| **Mockito** | testes | Cria dublês (mocks) de repositories/clients para testar os services isoladamente (testes unitários). |
| **Spring Boot Test + MockMvc** | testes | Testes de integração: sobem o contexto Spring completo e chamam a API via HTTP simulado. |
| **H2 (modo MySQL)** | testes | Banco em memória usado só nos testes, para rodarem sem Docker. |
| **Docker (multi-stage)** | todos | Empacota cada microserviço numa imagem: o 1º estágio compila com Maven, o 2º só leva o JRE e o `.jar` (imagem menor). |
| **Docker Compose** | raiz | Sobe todo o ambiente (2 bancos + 3 serviços) com um comando, na ordem certa (healthchecks + `depends_on`). |

Tipos de teste do projeto: **unitário** (`services/`, Mockito, sem Spring), **integração** (`integration/`, Spring +
H2), **cliente HTTP** (`client/`, `MockRestServiceServer`) e **contrato** (`contract/`, valida que o YAML OpenAPI é válido).

## Contrato primeiro (Swagger/OpenAPI)

O contrato de cada API é escrito à mão e é a **fonte de verdade** — a implementação o segue:

- [`ms-administrativo/.../static/contract/openapi-administrativo.yaml`](ms-administrativo/src/main/resources/static/contract/openapi-administrativo.yaml)
- [`ms-agendamento/.../static/contract/openapi-agendamento.yaml`](ms-agendamento/src/main/resources/static/contract/openapi-agendamento.yaml)

Com os serviços no ar, o Swagger UI exibe esses contratos:

- http://localhost:8081/swagger-ui.html (administrativo)
- http://localhost:8082/swagger-ui.html (agendamento)

Para criar uma API nova: **1)** escreva/atualize o YAML e combine com os outros times, **2)** implemente, **3)** teste.

## Pré-requisitos

- JDK **17** e Maven 3.9+ (`mvn -v` deve mostrar Java 17; com outra versão use `JAVA_HOME`)
- Docker (com Docker Compose)

## Compilar e testar

```bash
mvn clean verify
```

Roda os testes unitários (JUnit + Mockito), de integração e do contrato OpenAPI. Os de integração usam o contexto Spring completo com H2 em modo MySQL
(nos testes do agendamento, o `ms-administrativo` é simulado). Não precisa de MySQL/Docker para os testes.

## Gerar as imagens Docker

O contexto de build é a **raiz do repositório** (os serviços dependem do módulo `common`). O build Maven acontece
dentro da imagem (multi-stage), então não precisa gerar o `.jar` antes.

```bash
docker build -f ms-administrativo/Dockerfile -t clinical/ms-administrativo:latest .
docker build -f ms-agendamento/Dockerfile    -t clinical/ms-agendamento:latest .
docker build -f api-gateway/Dockerfile       -t clinical/api-gateway:latest .
```

## Executar no Docker

### Opção 1 — tudo de uma vez (docker compose)

```bash
docker compose up -d --build     # sobe 2 MySQL (um por serviço) + 3 serviços (aguarda os healthchecks)
docker compose ps
docker compose logs -f ms-agendamento
docker compose down              # para tudo (adicione -v para apagar o banco)
```

Se a porta 8080 do host estiver ocupada, troque a do gateway: `GATEWAY_PORT=8090 docker compose up -d --build`
(e use `localhost:8090` nos exemplos).

As imagens usam `eclipse-temurin:17-jre-jammy` (multi-arquitetura, roda em Apple Silicon e x86).

### Opção 2 — cada serviço como um container (`docker run`)

```bash
# rede para os containers se encontrarem pelo nome
docker network create clinical-net

# um banco por microserviço
docker run -d --name mysql-administrativo --network clinical-net -p 3307:3306 \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=clinical_administrativo \
  -e MYSQL_USER=clinical -e MYSQL_PASSWORD=clinical mysql:8.4

docker run -d --name mysql-agendamento --network clinical-net -p 3308:3306 \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=clinical_agendamento \
  -e MYSQL_USER=clinical -e MYSQL_PASSWORD=clinical mysql:8.4

# aguarde os bancos ficarem prontos (~20s):  docker logs mysql-administrativo  (procure "ready for connections")

docker run -d --name ms-administrativo --network clinical-net -p 8081:8081 \
  -e DB_URL="jdbc:mysql://mysql-administrativo:3306/clinical_administrativo?serverTimezone=UTC" \
  -e DB_USER=clinical -e DB_PASSWORD=clinical \
  clinical/ms-administrativo:latest

docker run -d --name ms-agendamento --network clinical-net -p 8082:8082 \
  -e DB_URL="jdbc:mysql://mysql-agendamento:3306/clinical_agendamento?serverTimezone=UTC" \
  -e DB_USER=clinical -e DB_PASSWORD=clinical \
  -e ADMINISTRATIVO_URL=http://ms-administrativo:8081 \
  clinical/ms-agendamento:latest

docker run -d --name api-gateway --network clinical-net -p 8080:8080 \
  -e ADMINISTRATIVO_URL=http://ms-administrativo:8081 \
  -e AGENDAMENTO_URL=http://ms-agendamento:8082 \
  clinical/api-gateway:latest
```

Parar e remover: `docker rm -f api-gateway ms-agendamento ms-administrativo mysql-agendamento mysql-administrativo && docker network rm clinical-net`

> Dentro da rede Docker os serviços se falam pelo **nome do container** (`ms-administrativo`, `mysql-administrativo`),
> nunca por `localhost`. Por isso as URLs são passadas por variável de ambiente.

### Variáveis de ambiente

| Variável | Serviço | Padrão (execução local) |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | administrativo, agendamento | administrativo: `localhost:3307/clinical_administrativo`; agendamento: `localhost:3308/clinical_agendamento`; usuário/senha `clinical` |
| `ADMINISTRATIVO_URL` | agendamento, gateway | `http://localhost:8081` |
| `AGENDAMENTO_URL` | gateway | `http://localhost:8082` |
| `SERVER_PORT` | todos | 8081 / 8082 / 8080 |

## Executar sem Docker para os serviços (desenvolvimento)

Suba só o banco e rode os serviços pela IDE ou Maven:

```bash
docker compose up -d mysql-administrativo mysql-agendamento
mvn -pl ms-administrativo spring-boot:run
mvn -pl ms-agendamento spring-boot:run     # em outro terminal
mvn -pl api-gateway spring-boot:run        # em outro terminal
```

## Testando a API (via gateway)

```bash
# 1. especialidade -> 2. médico -> 3. paciente
curl -s -X POST localhost:8080/administrativo/especialidades -H 'Content-Type: application/json' \
  -d '{"nome":"Cardiologia","descricao":"Coração"}'

curl -s -X POST localhost:8080/administrativo/medicos -H 'Content-Type: application/json' \
  -d '{"nome":"Maria Souza","cpf":"12345678901","dataNascimento":"1985-04-20","crm":"CRM-SP-1","especialidadeId":1}'

curl -s -X POST localhost:8080/administrativo/pacientes -H 'Content-Type: application/json' \
  -d '{"nome":"João Pereira","cpf":"98765432100","dataNascimento":"1990-01-15"}'

# 4. agendar consulta (o ms-agendamento valida médico e paciente no ms-administrativo)
curl -s -X POST localhost:8080/agendamento/consultas -H 'Content-Type: application/json' \
  -d '{"medicoId":1,"pacienteId":1,"dataAgendamento":"2030-05-10T14:30:00","sintomas":"dor de cabeça"}'

curl -s "localhost:8080/agendamento/consultas?status=AGENDADO"
```

Erros seguem sempre o mesmo formato (`timestamp`, `status`, `error`, `message`, `path`, `details[]`):
400 validação, 404 não encontrado, 409 conflito, 422 regra de negócio, 503 serviço dependente fora do ar.

## Logs (SLF4J)

Cada classe que registra logs declara o seu logger (SLF4J + Logback, já incluídos no Spring Boot):

```java
private static final Logger log = LoggerFactory.getLogger(MinhaClasse.class);

log.info("Criando médico CRM {}", crm);     // fluxo normal
log.warn("CPF já cadastrado");              // erro do cliente (4xx)
log.error("Falha ao chamar serviço", ex);   // falha nossa/integração (5xx), com stack trace
```

Use sempre `{}` (placeholders), nunca concatenação; não registre dados sensíveis (senhas, CPF completo).
Para ver mais detalhes: `LOGGING_LEVEL_BR_EDU_IMEPAC=DEBUG` (variável de ambiente do container).

## Estrutura de um módulo (use como modelo)

```
ms-<modulo>/
  Dockerfile
  src/main/java/br/edu/imepac/clinicalapi/<modulo>/
    entities/       entidades JPA (estendem BaseEntity do common)
    repositories/   Spring Data
    services/       regras de negócio, @Transactional, logs @Slf4j
    controllers/    endpoints REST (finos: só delegam ao service)
    dto/            Request/Response com Bean Validation
    exceptions/     GlobalExceptionHandler (@RestControllerAdvice) - tratamento central de erros
    mapper/         ModelMapper (DTO <-> entidade)
    client/         chamadas REST a outros módulos (ex.: AdministrativoClient)
    config/         beans (ModelMapper, RestClient, import do advice)
  src/main/resources/
    application.yml
    db/migration/   scripts Flyway (V1__..., V2__...)
    static/contract/openapi-<modulo>.yaml   contrato da API
  src/test/java/    services/ (Mockito) · integration/ (SpringBootTest + MockMvc) · client/
```

Checklist para um novo endpoint: contrato YAML → DTOs com validação → entidade + migration Flyway →
repository → service (lança `ResourceNotFoundException`/`ConflictException`/`BusinessRuleException`; **nunca**
monte resposta de erro no controller) → controller → testes unitário e de integração.

Fora do escopo desta entrega (modelados no diagrama, ficam para os alunos): Funcionário, Cargo, Convênio,
Procedimento, Insumo, Disponibilidade médica, Retorno e Exame (em `ms-agendamento`, estendem `Agendamento`),
além de `ms-atendimento`, `ms-estoque` e `ms-fatceira`.
