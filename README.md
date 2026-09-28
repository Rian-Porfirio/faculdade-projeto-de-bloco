# Sistema de Votação Evolutivo

Simulação acadêmica de urna/sistema de votação, desenvolvida em **três etapas**, cada uma em sua própria branch e
sempre em estado executável:

```text
main
├── branch-1  → REST + Spring Boot + PostgreSQL + React + testes      (esta versão)
├── branch-2  → Branch 1 + RabbitMQ / arquitetura orientada a eventos
└── branch-3  → Branch 2 + Docker, Kubernetes, observabilidade e CI/CD
```

> **Você está na Branch 1.** Docker, RabbitMQ, observabilidade e CI/CD **não** existem aqui de propósito:
> pertencem às etapas seguintes.

---

## 1. Visão geral

Um serviço Spring Boot expõe uma API REST para cadastrar eleitores e candidatos, registrar votos e apurar
resultados. Um frontend React consome essa API.

```text
┌──────────────┐   HTTP/JSON   ┌────────────────────────────────────────────┐   JDBC   ┌────────────┐
│ React (Vite) │ ────────────► │ voting-service (Spring Boot)               │ ───────► │ PostgreSQL │
│ :5173        │               │ Controller → Service → Repository (JPA)    │          │ :5432      │
└──────────────┘               └────────────────────────────────────────────┘          └────────────┘
```

Não é um sistema eleitoral real: não há biometria, criptografia eleitoral, autenticação nem regras do TSE.

---

## 2. Análise do domínio

### Entidades e relacionamentos

```text
 Partido 1 ────< Candidato >──── 1 Eleicao 1 ────< Voto >──── 1 Eleitor
                     │                               │             │
                     └───────────── 1 ───────────────┘             │
                                    (Voto → Candidato)             │
                                                                   N
                                LocalVotacao 1 ────< Eleitor  ─────┘
                                LocalVotacao 1 ────< Voto  (onde o voto foi dado)
```

| Entidade | Principais atributos | Observação |
|---|---|---|
| `Partido` | sigla (única), nome, número | Todo candidato pertence a um partido |
| `Eleicao` | nome, descrição, início, término, status | Status: `AGENDADA`, `ATIVA`, `ENCERRADA`. Só `ATIVA` aceita votos |
| `LocalVotacao` | nome, cidade, estado, zona | Zona pertence ao local (evita repetir em cada eleitor) |
| `Eleitor` | nome, identificador (único), estado, cidade, local de votação | Identificador simula o título de eleitor |
| `Candidato` | nome, número, cargo, partido, eleição, estado, cidade | `Regiao` é **derivada** da UF, não persistida |
| `Voto` | eleitor, candidato, eleição, local, data/hora | Constraint única `(eleitor, eleição)` |

`Cargo` (presidente, governador, senador, deputado federal/estadual, prefeito, vereador) e `StatusEleicao` são
enums. `Regiao` (Norte, Nordeste, Centro-Oeste, Sudeste, Sul) é calculada a partir da UF.

### Decisões de modelagem (justificativas)

* **`Eleicao` existe** porque a regra "um voto por eleição" e a de "eleição ativa" exigem esse conceito.
  Mantida mínima.
* **Cargo e Região não são tabelas.** Cargo é um conjunto fixo (enum). Região é função da UF (evita
  redundância e inconsistência).
* **Regra do voto único garantida em duas camadas:** verificação no service (mensagem clara) e constraint
  única no banco (protege contra requisições concorrentes).
* **Simplificação assumida:** um eleitor tem *um* voto por eleição (como pedido no enunciado), mesmo com
  candidatos de cargos diferentes.
* **"Votos por região"** = votos de eleitores residentes no estado consultado (de onde vem o voto).
* **Data/hora do voto** é gerada no servidor (`Clock` injetável), nunca enviada pelo cliente.
* Sem autenticação: fora do escopo. O voto é consultável por eleitor (exigência do enunciado), o que
  não preserva o sigilo, aceitável numa simulação acadêmica.

### Regras de negócio implementadas

| Regra | Onde | Resposta HTTP |
|---|---|---|
| Eleitor, candidato e eleição precisam existir | `VotoService` | 404 |
| Eleição precisa estar `ATIVA` | `VotoService` | 422 |
| Candidato precisa concorrer na eleição do voto | `VotoService` | 422 |
| Eleitor não vota duas vezes na mesma eleição | `VotoService` + constraint | 409 |
| Voto tem data/hora e eleição | `VotoService` | — |
| Candidato tem partido e cargo obrigatórios | Bean Validation + `CandidatoService` | 400 / 404 |
| Identificador de eleitor único; número de candidato único por cargo/UF/eleição | Services | 409 |
| Não remover eleitor que votou nem candidato que recebeu votos | Services | 409 |
| UF válida (27 UFs) | `Regiao` | 422 |

### Fora do escopo desta etapa

RabbitMQ e eventos (Branch 2); Docker, Kubernetes, observabilidade, CI/CD e testes E2E (Branch 3); autenticação;
regras eleitorais reais.

---

## 3. Estrutura do projeto

```text
backend/  (Spring Boot 3.3 · Java 21)
└── src/main/java/br/edu/votacao/
    ├── controller/   HTTP apenas (sem regra de negócio)
    ├── service/      regras de negócio e transações
    ├── repository/   Spring Data JPA
    ├── domain/       entidades e enums
    ├── dto/          records de entrada/saída + Bean Validation
    ├── mapper/       conversão entidade ↔ DTO (manual, didática)
    ├── exception/    exceções e @RestControllerAdvice
    └── config/       CORS, OpenAPI, Clock, dados de demonstração
frontend/  (React 19 · Vite · TypeScript)
└── src/{api,pages,components}
```

---

## 4. API REST (`/api/v1`)

Documentação interativa (Swagger UI): <http://localhost:8080/swagger-ui.html>

| Recurso | Endpoints |
|---|---|
| Eleitores | `POST/GET /eleitores` · `GET/PUT/DELETE /eleitores/{id}` · `GET /eleitores/{id}/votos` |
| Candidatos | `POST/GET /candidatos` (`?eleicaoId=`) · `GET/PUT/DELETE /candidatos/{id}` · `GET /candidatos/{id}/votos` · `GET /candidatos/{id}/resultado` |
| Votos | `POST /votos` · `GET /votos` (`?eleicaoId=`) · `GET /votos/{id}` · `GET /votos/eleitor/{id}` · `GET /votos/candidato/{id}` |
| Resultados | `GET /resultados` (`?eleicaoId=&cargo=&estado=`) · `/resultados/candidatos/{id}` · `/resultados/cargos/{cargo}` · `/resultados/regioes/{uf}` |
| Apoio | `GET/POST /partidos` · `GET/POST /locais-votacao` · `GET/POST/PUT /eleicoes`, `GET /eleicoes/{id}` |

Os endpoints de apoio existem porque candidatos e eleitores dependem deles (partido, local, eleição) e o
frontend precisa preenchê-los. Sem `eleicaoId`, os resultados usam a eleição ativa.

Exemplo de voto:

```bash
curl -X POST http://localhost:8080/api/v1/votos -H 'Content-Type: application/json' \
  -d '{"eleitorId":1,"candidatoId":1,"eleicaoId":1}'
```

Erros seguem um formato único: `{ timestamp, status, error, message, path, details[] }`.

---

## 5. Como executar

Pré-requisitos: **JDK 21**, **Maven 3.9+**, **Node 20+**. PostgreSQL 14+ (ou use o profile `h2`).

### Backend com PostgreSQL

```bash
# 1) criar o banco (uma vez)
createdb -U postgres votacao

# 2) subir a API (usa localhost:5432, usuário/senha postgres/postgres por padrão)
cd backend
mvn spring-boot:run
# ou, com outras credenciais:
DATABASE_URL=jdbc:postgresql://localhost:5432/votacao DATABASE_USERNAME=... DATABASE_PASSWORD=... mvn spring-boot:run
```

### Backend sem PostgreSQL (demonstração rápida)

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Na primeira execução com banco vazio, `DataSeeder` cria dados **fictícios** (3 partidos, 3 locais,
2 eleições — uma ativa e uma encerrada —, 5 candidatos e 6 eleitores). Desative com `app.seed.enabled=false`.

### Frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173  (encaminha /api para localhost:8080)
```

---

## 6. Testes

```bash
cd backend  && mvn test        # JUnit 5 + Mockito + Spring Boot Test (H2 em memória)
cd frontend && npm test        # Vitest + Testing Library
```

**Backend**

| Tipo | Classes | Cobre |
|---|---|---|
| Unitários de Service | `VotoServiceTest`, `ResultadoServiceTest`, `EleitorServiceTest` | voto com sucesso, voto duplicado, candidato/eleitor inexistente, eleição inativa, candidato de outra eleição, votos por eleitor/candidato, cálculo de resultado, filtros |
| Controllers (`@WebMvcTest`) | `VotoControllerTest`, `EleitorControllerTest`, `CandidatoControllerTest`, `ResultadoControllerTest` | status HTTP, validação (400), mapeamento de erros (404/409/422) |
| Repository (`@DataJpaTest`) | `VotoRepositoryTest` | consultas, agregações, constraint única |
| Domínio | `RegiaoTest` | UF → região |
| Fluxo | `VotacaoFluxoIntegrationTest` | HTTP → JPA → H2: votar, bloquear 2º voto, apurar |

**Frontend (23 testes):** cliente HTTP e tratamento de erros, tela de votação (fluxo completo, erro 409,
corrigir escolha, sem eleição ativa), resultados (percentuais, filtros, erro), CRUD de candidatos e eleitores.

---

## 7. Roteiro de demonstração sugerido

1. Abrir **Votação**, escolher *Fernanda Lima* e *Ana Ribeiro*, confirmar → comprovante com data/hora.
2. Tentar votar de novo com a mesma eleitora → mensagem de erro (409).
3. Abrir **Resultado**, filtrar por cargo e por estado.
4. Em **Eleitores**, clicar em *Votos* para ver o histórico.
5. Mostrar o Swagger UI e a separação Controller → Service → Repository no código.

---

## 8. Próximas etapas

* **Branch 2:** eventos `VotoRegistrado` etc. publicados no RabbitMQ; consumidores (resultado/auditoria).
* **Branch 3:** Docker, Compose, Kubernetes, Actuator/Prometheus/Grafana, tracing, GitHub Actions, Testcontainers/E2E.
