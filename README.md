# AgrotelliSystem — Sistema CRUD com Interface Web

Sistema completo de gestão de produtos desenvolvido com **Java 21**, **Spring Boot 3.3**, **Thymeleaf + HTMX + Bootstrap 5** e suíte avançada de testes automatizados.

---

## Sumário

- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Como Executar](#como-executar)
- [Executar os Testes](#executar-os-testes)
- [Cobertura de Testes](#cobertura-de-testes)
- [Testes Selenium (E2E)](#testes-selenium-e2e)
- [Fuzz Testing](#fuzz-testing)
- [Decisões de Arquitetura](#decisões-de-arquitetura)]
- [Coverage Report (Sobre os Testes)](COVERAGE_REPORT.md)

---

## Tecnologias

| Camada        | Tecnologia                        |
|---------------|-----------------------------------|
| Backend       | Java 21 + Spring Boot 3.3         |
| Build         | Gradle (Groovy DSL)               |
| Persistência  | Spring Data JPA + H2 (in-memory)  |
| Frontend      | Thymeleaf + HTMX + Bootstrap 5    |
| Tipografia    | Poppins (títulos) + Source Sans 3 |
| Testes Unit.  | JUnit 5 + Mockito                 |
| Testes Integr.| Spring Boot Test + MockMvc        |
| Testes E2E    | Selenium 4 + WebDriverManager     |
| Fuzz Testing  | JUnit 5 Parametrizado             |
| Cobertura     | JaCoCo (mínimo 85%)               |

---

## Estrutura do Projeto

```
crud-system/
├── src/
│   ├── main/
│   │   ├── java/com/crud/
│   │   │   ├── CrudApplication.java
│   │   │   ├── config/          # DataInitializer
│   │   │   ├── controller/      # ProductController
│   │   │   ├── dto/             # ProductDTO
│   │   │   ├── exception/       # BusinessException, ProductNotFoundException, GlobalExceptionHandler
│   │   │   ├── model/           # Product (JPA Entity)
│   │   │   ├── repository/      # ProductRepository
│   │   │   └── service/         # ProductService
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── fragments/   # navbar, alerts, product-table, footer
│   │       │   ├── products/    # list.html, form.html
│   │       │   └── error/       # error.html
│   │       ├── static/
│   │       │   ├── css/style.css
│   │       │   └── js/app.js
│   │       └── application.properties
│   └── test/
│       ├── java/com/crud/
│       │   ├── service/         # ProductServiceTest (unitário)
│       │   ├── controller/      # ProductControllerTest, GlobalExceptionHandlerTest
│       │   ├── integration/     # ProductIntegrationTest
│       │   ├── fuzz/            # ProductFuzzTest
│       │   └── selenium/
│       │       ├── BaseSeleniumTest.java
│       │       ├── ProductSeleniumTest.java
│       │       ├── pages/       # BasePage, ProductListPage, ProductFormPage
│       │       └── components/  # NavbarComponent, DeleteModalComponent
│       └── resources/application-test.properties
├── build.gradle
├── settings.gradle
└── README.md
```

---

## Como Executar

### Pré-requisitos

- Java 21 instalado e `JAVA_HOME` configurado
- Gradle (ou use o wrapper `./gradlew`)

### Iniciar a aplicação

```bash
# Via Gradle
./gradlew bootRun

# Ou compilar e executar o JAR
./gradlew build
java -jar build/libs/crud-system-1.0.0.jar
```

Acesse: [http://localhost:8080/products](http://localhost:8080/products)

---

## Executar os Testes

### Todos os testes (exceto Selenium E2E)

```bash
./gradlew test
```

### Testes por categoria

```bash
# Apenas testes unitários do service
./gradlew test --tests "com.crud.service.*"

# Apenas testes do controller (MockMvc)
./gradlew test --tests "com.crud.controller.*"

# Apenas testes de integração
./gradlew test --tests "com.crud.integration.*"

# Apenas fuzz tests
./gradlew test --tests "com.crud.fuzz.*"

# Testes Selenium (requer Chrome instalado)
./gradlew test --tests "com.crud.selenium.*"
```

### Gerar relatório de cobertura (JaCoCo)

```bash
./gradlew jacocoTestReport
```

Relatório HTML gerado em: `build/reports/jacoco/test/html/index.html`

### Verificar cobertura mínima (85%)

```bash
./gradlew jacocoTestCoverageVerification
```

---

## Cobertura de Testes

| Camada     | Estratégia                                          |
|------------|-----------------------------------------------------|
| Service    | Testes unitários com Mockito, todos os branches     |
| Controller | MockMvc: sucesso, validação, erros de negócio       |
| Repository | Cobertura via testes de integração com H2           |
| Exception  | GlobalExceptionHandler testado com MockMvc          |
| Fuzz       | 50+ entradas maliciosas/limite em todos os campos   |
| E2E        | 15 cenários Selenium cobrindo todos os fluxos CRUD  |

---

## Testes Selenium (E2E)

Os testes Selenium usam **Page Object Model (POM)** com hierarquia clara:

```
BaseSeleniumTest          ← WebDriver setup/teardown
BasePage                  ← Esperas, utilitários
  ├── ProductListPage     ← /products
  ├── ProductFormPage     ← /products/new e /products/{id}/edit
  └── components/
      ├── NavbarComponent       ← Navegação
      └── DeleteModalComponent  ← Modal de confirmação
```

### Pré-requisito para Selenium

- Google Chrome instalado
- WebDriverManager baixa o ChromeDriver automaticamente

### Executar Selenium

```bash
# Com servidor já rodando (porta 8080)
./gradlew test --tests "com.crud.selenium.*"

# Ou com SpringBootTest (sobe o servidor automaticamente)
./gradlew test --tests "com.crud.selenium.ProductSeleniumTest"
```

Os testes rodam em modo **headless** por padrão. Para ver o navegador:

```java
// Em BaseSeleniumTest.java, remova a linha:
options.addArguments("--headless=new");
```

---

## Fuzz Testing

O `ProductFuzzTest` cobre:

| Categoria         | Entradas testadas                                                   |
|-------------------|---------------------------------------------------------------------|
| Nomes maliciosos  | XSS, SQL Injection, Path Traversal, Template Injection, Emoji      |
| Preços limite     | Zero, negativo, máximo+1, overflow, escala extra                    |
| Estoques limite   | Negativo, zero, máximo+1, Integer.MAX_VALUE, Integer.MIN_VALUE      |
| Categorias        | Vazia, espaço, XSS, SQL Injection, acima do limite                 |
| IDs extremos      | Zero, negativo, Long.MAX_VALUE, Long.MIN_VALUE                     |
| Buscas maliciosas | SQL Injection, XSS, strings de 1000 chars                         |

Todos os casos validam **fail gracefully** — nenhum deve lançar exceções não controladas.

---

## Decisões de Arquitetura

### Fail Early
- Validação de DTO com Bean Validation (`@Valid`) na camada de controller
- Verificação de ID nulo/negativo no início do `findById`
- `@PrePersist` / `@PreUpdate` garantem timestamps sempre preenchidos

### Fail Gracefully
- `GlobalExceptionHandler` captura todas as exceções e retorna views amigáveis
- Mensagens de erro nunca expõem stack trace ou informações internas
- Erros de negócio retornam ao formulário com feedback contextual

### Segurança nas mensagens de erro
- `server.error.include-stacktrace=never`
- `server.error.include-message=never`
- Mensagens genéricas para erros 500 (sem detalhes internos)

### Modularidade
- Fragmentos Thymeleaf separados: `navbar`, `alerts`, `product-table`, `footer`
- HTMX para atualizações parciais da tabela sem reload completo
- Page Objects Selenium reutilizáveis por componente e por página

### H2 Console (desenvolvimento)
Acesse [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- JDBC URL: `jdbc:h2:mem:cruddb`
- Usuário: `sa` | Senha: *(vazio)*
