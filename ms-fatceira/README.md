# Módulo fatceira

Este módulo faz parte do projeto `clinicalapi` e segue o pacote base:

`br.edu.imepac.clinicalapi.fatceira`

## Estrutura principal

```text
src/
  main/
    java/
      br/
        edu/
          imepac/
            clinicalapi/
              fatceira/
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
