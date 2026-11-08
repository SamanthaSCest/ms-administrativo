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
