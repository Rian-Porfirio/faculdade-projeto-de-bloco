# Relatório de Cobertura e Vulnerabilidades — AgrotelliSystem

## Resumo Executivo

| Métrica                        | Valor                     |
|-------------------------------|---------------------------|
| Meta de cobertura             | ≥ 85%                     |
| Ferramenta de cobertura       | JaCoCo                    |
| Total de classes testadas     | 8 (service, controller, repository, exception, config, model, dto, validation) |
| Total de arquivos de teste    | 18 arquivos Java          |
| Total de casos de teste       | ~120 cenários             |

---

## Estratégia de Cobertura por Camada

### 1. Service Layer — `ProductService`
**Arquivo:** `ProductServiceTest.java`
**Tipo:** Unitário com Mockito
**Cobertura estimada:** 100% de branches

| Método         | Cenários cobertos                                                         |
|----------------|---------------------------------------------------------------------------|
| `findAll`      | Lista com itens; lista vazia                                              |
| `search`       | Parâmetros nulos; termo vazio; combinações search+category                |
| `findById`     | Produto encontrado; não encontrado (404); ID nulo; ID negativo/zero       |
| `create`       | Sucesso; nome duplicado (fail early)                                      |
| `update`       | Sucesso; produto não encontrado; mesmo nome do próprio produto            |
| `delete`       | Sucesso; produto não encontrado                                           |
| `findAllCategories` | Lista retornada                                                      |

### 2. Controller Layer — `ProductController` + `HomeController`
**Arquivos:** `ProductControllerTest.java`, `HomeControllerTest.java`, `GlobalExceptionHandlerTest.java`
**Tipo:** MockMvc (`@WebMvcTest`)
**Cobertura estimada:** 95%+ de branches

| Endpoint            | Cenários cobertos                                                        |
|---------------------|--------------------------------------------------------------------------|
| `GET /`             | Redirecionamento para `/products`                                        |
| `GET /products`     | Listagem padrão; busca com parâmetro; sem resultados                    |
| `GET /products/table` | Fragmento HTMX retornado                                               |
| `GET /products/new` | Formulário vazio                                                         |
| `POST /products`    | Criação válida; dados inválidos; nome duplicado; preço inválido (parametrizado); estoque inválido (parametrizado) |
| `GET /products/{id}/edit` | Produto encontrado; não encontrado; ID não numérico (400)         |
| `POST /products/{id}` | Atualização válida; dados inválidos; produto não encontrado            |
| `DELETE /products/{id}` | Exclusão bem-sucedida; produto não encontrado                        |

### 3. Repository Layer — `ProductRepository`
**Arquivo:** `ProductRepositoryTest.java`
**Tipo:** `@DataJpaTest` com H2
**Cobertura estimada:** 100% dos métodos custom

| Método                      | Cenários                                                        |
|----------------------------|-----------------------------------------------------------------|
| `findByNameContaining`     | Match parcial; case insensitive; sem resultado                  |
| `findByCategoryIgnoreCase` | Categoria existente; categoria inexistente                      |
| `findBySearchAndCategory`  | 5 combinações parametrizadas                                    |
| `existsByNameIgnoreCase`   | Nome existente; nome inexistente; case insensitive              |
| `findAllCategories`        | Distinção e ordenação verificadas                               |
| CRUD básico JPA            | save, findById, delete, update                                  |

### 4. DTO Validation — `ProductDTO`
**Arquivo:** `ProductDTOValidationTest.java`
**Tipo:** Validação direta com `javax.validation.Validator`
**Cobertura:** 100% das anotações de validação

| Campo         | Cenários                                                          |
|---------------|-------------------------------------------------------------------|
| `name`        | Nulo, vazio, 1 char, 101 chars, 2 chars (min), 100 chars (max)  |
| `description` | 501 chars, nulo (válido), 500 chars                              |
| `price`       | Nulo, zero, negativo, 1.000.000, 0.01 (min), 999.999,99 (max)   |
| `stock`       | Nulo, -1, 100.001, MAX_VALUE, 0, 100.000                        |
| `category`    | Nulo, vazio, em branco, 51 chars, 50 chars                       |

### 5. Fuzz Testing — `ProductFuzzTest`
**Arquivo:** `ProductFuzzTest.java`
**Tipo:** Spring Boot Test parametrizado

| Categoria de entrada | Entradas testadas                                            |
|---------------------|--------------------------------------------------------------|
| Nomes maliciosos    | 20+ entradas: XSS, SQL Injection, Path Traversal, Template Injection, Emoji, URL Encoding |
| Preços limite       | 7 valores: zero, negativo, overflow, escala inválida         |
| Estoque limite      | 7 valores: negativos, MAX_VALUE, MIN_VALUE                   |
| Categorias          | 6 entradas: vazia, XSS, SQL Injection, acima do limite, nula |
| IDs extremos        | 4 valores: zero, negativo, Long.MAX_VALUE, Long.MIN_VALUE    |
| Buscas maliciosas   | 6 entradas: SQL Injection, XSS, string de 1000 chars         |

**Resultado esperado:** Todas as entradas devem ser tratadas de forma controlada (`fail gracefully`). Nenhuma deve propagar exceções inesperadas como `NullPointerException`, `StackOverflowError` ou `OutOfMemoryError`.

### 6. Integration Tests — `ProductIntegrationTest`
**Arquivo:** `ProductIntegrationTest.java`
**Tipo:** `@SpringBootTest` + H2 + `@Transactional`

| Cenário                                         | ID    |
|-------------------------------------------------|-------|
| Criar e buscar produto — ciclo básico           | INT-01 |
| Atualizar produto — verificar persistência      | INT-02 |
| Excluir produto — verificar ausência            | INT-03 |
| Prevenir duplicata de nome (fail early)         | INT-04 |
| Busca com filtro de nome                        | INT-05 |
| Busca por categoria                             | INT-06 |
| Timestamps preenchidos automaticamente          | INT-07 |
| findAllCategories — valores distintos           | INT-08 |
| Fail gracefully — ID inválido                   | INT-09 |
| Produto com estoque zero                        | INT-10 |

### 7. Selenium E2E — `ProductSeleniumTest`
**Arquivo:** `ProductSeleniumTest.java` + Page Objects
**Tipo:** `@SpringBootTest(RANDOM_PORT)` + Chrome Headless

| Cenário                                                          | ID     |
|------------------------------------------------------------------|--------|
| Página de listagem carrega com tabela visível                    | SEL-01 |
| Navbar exibe marca e botão                                       | SEL-02 |
| Criar produto válido — redirecionamento e sucesso                | SEL-03 |
| Criar produto sem nome — erro de validação                       | SEL-04 |
| Campos obrigatórios inválidos — erros individuais (parametrizado)| SEL-05 |
| Editar produto existente — salvar e confirmar sucesso            | SEL-06 |
| Cancelar edição — retorna sem alterações                         | SEL-07 |
| Modal de exclusão exibe nome do produto                          | SEL-08 |
| Confirmar exclusão remove da lista                               | SEL-09 |
| Cancelar exclusão mantém produto                                 | SEL-10 |
| Busca por nome filtra resultados                                 | SEL-11 |
| Busca sem resultados exibe empty state                           | SEL-12 |
| Limpar filtro restaura todos os produtos                         | SEL-13 |
| Botão "Novo Produto" na navbar abre formulário                   | SEL-14 |
| Contador de caracteres da descrição atualiza                     | SEL-15 |

---

## Vulnerabilidades Identificadas e Mitigações

| # | Vulnerabilidade              | Risco   | Mitigação Implementada                                    |
|---|------------------------------|---------|-----------------------------------------------------------|
| 1 | XSS via campos de formulário | Alto    | Thymeleaf escapa HTML por padrão em `th:text` e `th:field` |
| 2 | SQL Injection via busca      | Alto    | Spring Data JPA com parâmetros nomeados (`@Query` com `:param`) |
| 3 | Exposição de stack trace     | Médio   | `server.error.include-stacktrace=never` + GlobalExceptionHandler com mensagens genéricas |
| 4 | Exposição de mensagem interna| Médio   | `server.error.include-message=never` |
| 5 | Double submit                | Baixo   | Botão desabilitado via JS após primeiro clique            |
| 6 | Estoque negativo             | Baixo   | `@Min(0)` na validação do DTO + Bean Validation           |
| 7 | Preço inválido (overflow)    | Baixo   | `@DecimalMax("999999.99")` + escala controlada no banco   |
| 8 | Nome duplicado               | Baixo   | Verificação explícita no `ProductService` (fail early)    |
| 9 | Path Traversal em ID         | Médio   | Tipagem forte (`Long id`) + `MethodArgumentTypeMismatchException` → 400 |

---

## Como Gerar o Relatório de Cobertura

```bash
# Executar todos os testes e gerar relatório JaCoCo
./gradlew test jacocoTestReport

# Abrir relatório no browser
open build/reports/jacoco/test/html/index.html

# Verificar se meta de 85% é atingida (falha o build se não atingir)
./gradlew jacocoTestCoverageVerification
```

O relatório HTML fica em:
```
build/reports/jacoco/test/html/index.html
```

O relatório XML (para CI) fica em:
```
build/reports/jacoco/test/jacocoTestReport.xml
```
