# Módulo administrativo (MS-ADM)

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
                ApplicationAdministrativo.java
                config/
                controllers/
                dto/
                entities/
                exceptions/
                mapper/
                repositories/
                services/
                utils/
                validation/
    resources/
      application.properties
      application-docker.properties
      db/migration/
  test/
    java/  (mesma árvore de pacotes do main)
```

## Convenções

- `entities`: entidades de domínio / JPA (inclui `EntidadeBase`, `Endereco`, `UF` e `OutboxEvento`)
- `repositories`: interfaces de persistência e consultas (JPA e `JdbcTemplate`)
- `services`: regras de negócio (inclui a gravação e a publicação dos eventos da outbox)
- `controllers`: endpoints REST
- `utils`: utilitários e helpers
- `dto`: request/response da API e o envelope dos eventos
- `mapper`: conversão entre DTOs e entidades
- `exceptions`: exceções e tratamento de erros
- `config`: configurações do Spring e beans (auditoria, Jackson da outbox, RabbitMQ)
- `validation`: validações e regras específicas

## Como rodar

```bash
docker compose up --build
```

A API sobe na porta 8080, com o Swagger em `/swagger-ui.html` e a saúde em `/actuator/health`.

## Observações

- Estrutura alinhada ao repositório da turma (`clinical-back-2026-02`).
- Novos pacotes podem ser adicionados conforme a funcionalidade evoluir.
