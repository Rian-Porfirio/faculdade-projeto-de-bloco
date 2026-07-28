# Sistema de Biblioteca — Camada de Persistência (2ª Etapa)

Projeto Integrado da disciplina de Engenharia de Softwares Escaláveis.
Esta etapa implementa a camada de persistência da aplicação usando JPA,
Spring Data JPA, Hibernate e auditoria com Hibernate Envers.

## Tecnologias

- Java 17
- Spring Boot 3.3.5 (Spring Data JPA)
- Hibernate ORM + Hibernate Envers (histórico/auditoria)
- Banco H2 em memória (desenvolvimento e testes)
- JUnit 5 + Mockito (testes)

## Como executar

Rodar a aplicação:

```
mvn spring-boot:run
```

Rodar a suíte de testes:

```
mvn test
```

Console do H2 (com a aplicação no ar): http://localhost:8080/h2-console
JDBC URL: `jdbc:h2:mem:biblioteca`

## Estrutura

```
model/        entidades JPA, @Embeddable e enum
repository/   interfaces Spring Data (JpaRepository)
service/      regras de negócio, auditoria e exceções
test/         testes de repositório, de serviço e de auditoria
```

A documentação detalhada do design da camada de persistência, do modelo de
dados e dos repositórios está em `DOCUMENTACAO.md`.

## Enviando para o repositório remoto

O histórico de commits já está criado localmente. Para publicar no repositório
da disciplina, ajuste o autor para o seu e envie:

```
git config user.name "Seu Nome"
git config user.email "seu.email@exemplo.com"
git remote add origin <URL_DO_SEU_REPOSITORIO>
git push -u origin main
```

Se o repositório remoto já existir da 1ª etapa, basta copiar estes arquivos
para dentro dele, dar `git add .` e `git commit`.
