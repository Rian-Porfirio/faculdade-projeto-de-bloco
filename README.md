# Sistema de Votação Evolutivo

Simulação acadêmica de urna/sistema de votação, desenvolvida em **três etapas**, cada uma em sua própria branch e
sempre em estado executável:

```text
main
├── branch-1  → REST + Spring Boot + PostgreSQL + React + testes
├── branch-2  → Branch 1 + RabbitMQ / arquitetura orientada a eventos   (esta versão)
└── branch-3  → Branch 2 + Docker, Kubernetes, observabilidade e CI/CD
```

> **Você está na Branch 2.** Tudo da Branch 1 continua funcionando; a novidade é a arquitetura orientada a eventos
> (seção 9). Docker, Kubernetes, observabilidade e CI/CD **não** existem aqui de propósito (Branch 3).

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
# (Branch 2: também result-service e audit-service, ver seção 9.10)
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

## 8. Branch 1 em uma frase

Frontend React → API REST (Spring Boot) → PostgreSQL. Tudo síncrono, em um único serviço. As seções 1 a 7
descrevem essa base, que a Branch 2 **não recria**: apenas evolui.

---

## 9. Branch 2 — arquitetura orientada a eventos (RabbitMQ)

### 9.1 O que mudou

```text
                          ┌──────────────────────────────────────────────────────────────┐
                          │                        RabbitMQ                              │
                          │                                                              │
 React ──REST──► voting-service ──publica──► votacao.events (topic)                      │
   │            (:8080, PostgreSQL              ├─ voto.* / candidato.* / eleicao.* ─► result.queue ──► result-service (:8081)
   │             "votacao")                     │                                     │                  PostgreSQL "resultados"
   │                                            └─ #  (todos) ────────────────────► audit.queue  ──► audit-service  (:8082)
   │                                                                                                      PostgreSQL "auditoria"
   │                          falha após 3 tentativas ─► votacao.dlx ─► result.dlq / audit.dlq
   │
   ├──── consulta resultados ──► result-service  (projeção por eventos)   ┐ a tela "Resultado" permite
   └──── consulta resultados ──► voting-service  (consulta direta)        ┘ alternar e comparar as duas fontes
```

| Serviço | Responsabilidade | Banco |
|---|---|---|
| `voting-service` (pasta `backend/`) | Fonte da verdade: CRUD, regras e registro de votos. **Publica** eventos. | `votacao` |
| `result-service` | **Consome** eventos e mantém uma projeção (read model) de votos por candidato/estado. Serve consultas de resultado. | `resultados` |
| `audit-service` | **Consome todos** os eventos e mantém uma trilha de auditoria consultável. | `auditoria` |

Não foram criados serviços sem finalidade: cada consumidor responde a uma necessidade distinta (leitura de
resultado × trilha de auditoria) e pode falhar, escalar e evoluir de forma independente. Cada serviço tem seu
próprio banco (mesmo servidor PostgreSQL, bancos separados), sem acesso cruzado.

### 9.2 Eventos escolhidos (e os que ficaram de fora)

| Evento | Routing key | Quem publica / quando | Consumidores |
|---|---|---|---|
| `VotoRegistrado` | `voto.registrado` | `VotoService`, após gravar o voto | result, audit |
| `CandidatoCadastrado` | `candidato.cadastrado` | `CandidatoService.criar` | result, audit |
| `CandidatoAtualizado` | `candidato.atualizado` | `CandidatoService.atualizar` | result, audit |
| `CandidatoRemovido` | `candidato.removido` | `CandidatoService.remover` | result, audit |
| `EleicaoIniciada` | `eleicao.iniciada` | `EleicaoService`, quando o status passa a `ATIVA` | result, audit |
| `EleicaoFinalizada` | `eleicao.finalizada` | `EleicaoService`, quando passa a `ENCERRADA` | result, audit |

**Ficou de fora:** `EleitorCadastrado`. Nenhum consumidor precisa dele (o resultado só precisa do estado do eleitor,
que já vai dentro de `VotoRegistrado`); publicá-lo seria evento "por publicar". Os eventos de candidato existem
porque o `result-service` precisa exibir candidatos sem nenhum voto e manter nomes atualizados sem chamar a API.

### 9.3 Formato das mensagens

Todas as mensagens usam o mesmo envelope JSON (`content-type: application/json`, mensagens persistentes):

```json
{
  "eventId": "5b0c8b7e-0d0e-3b5a-9a3f-1f2f4c9c7a10",
  "eventType": "VotoRegistrado",
  "occurredAt": "2026-09-27T20:30:00Z",
  "version": 1,
  "data": {
    "votoId": 42, "eleitorId": 3, "estadoEleitor": "SP",
    "candidatoId": 1, "candidatoNome": "Ana Ribeiro", "candidatoNumero": 10,
    "cargo": "PRESIDENTE", "partidoSigla": "AZL",
    "eleicaoId": 1, "eleicaoNome": "Eleição Simulada 2026", "localVotacaoId": 1,
    "dataHora": "2026-09-27T20:30:00Z"
  }
}
```

* `eventId`: chave de idempotência. Para votos é **determinístico** (derivado do `votoId`), então republicar o
  mesmo voto gera o mesmo id.
* `version`: versão do formato. Consumidores rejeitam versões que não conhecem (`InvalidEventException`) e
  **toleram campos novos** (`ignoreUnknown`), permitindo evoluir o contrato sem quebrar quem consome.
* Dados **denormalizados** no payload (nome do candidato, cargo, estado do eleitor…): o consumidor não precisa
  chamar o `voting-service` (desacoplamento). Não trafegam nomes de eleitores.
* Cada serviço define suas **próprias classes** de payload (contrato por JSON, não por biblioteca compartilhada).

### 9.4 Topologia RabbitMQ

| Item | Nome | Detalhe |
|---|---|---|
| Exchange | `votacao.events` | `topic`, durável |
| Fila | `result.queue` | bindings `voto.*`, `candidato.*`, `eleicao.*` |
| Fila | `audit.queue` | binding `#` (recebe tudo) |
| Exchange de falhas | `votacao.dlx` | `direct` |
| Filas de falhas | `result.dlq`, `audit.dlq` | recebem mensagens rejeitadas |

Como o exchange é `topic`, um único evento é entregue a **múltiplos consumidores** (cada fila recebe sua cópia).
O `voting-service` declara toda a topologia (assim nenhum evento se perde se ele subir antes dos consumidores) e
cada consumidor redeclara a sua fila com argumentos idênticos. Em produção isso ficaria em IaC/`definitions.json`.

### 9.5 Fluxo de um voto

1. `POST /api/v1/votos` → `VotoService` valida, grava o voto e chama `DomainEventPublisher.votoRegistrado(...)`.
2. O `DomainEventPublisher` publica um evento **interno do Spring**; os services não conhecem o RabbitMQ.
3. Após o **commit** da transação, `RabbitEventRelay` (`@TransactionalEventListener(AFTER_COMMIT)`) envia o
   envelope à exchange `votacao.events` com a routing key `voto.registrado`.
4. O RabbitMQ entrega uma cópia a `result.queue` e outra a `audit.queue`.
5. `result-service`: incrementa a contagem (candidato × estado do eleitor) e grava o `eventId` na mesma transação.
   `audit-service`: grava o evento na trilha.
6. O frontend consulta o resultado no `result-service` (eventualmente consistente) ou no `voting-service`.

Publicar **depois do commit** evita anunciar um voto que acabou desfeito por rollback.

### 9.6 Idempotência, erros e reprocessamento

| Preocupação | Como é tratada |
|---|---|
| **Mensagem duplicada** | `result-service` guarda `eventId` em `evento_processado` (mesma transação da alteração); `audit-service` tem `event_id` único. Duplicatas são ignoradas. |
| **Mensagem inválida** (JSON quebrado, versão ≠ 1, campo obrigatório ausente) | `InvalidEventException` (`AmqpRejectAndDontRequeueException`) → vai para a **DLQ**. |
| **Falha transitória** (ex.: banco fora) | Retry do Spring (3 tentativas, 1s e 2s); esgotadas, a mensagem vai para a DLQ em vez de ficar num loop infinito. |
| **Evento desconhecido** | Ignorado com aviso no log (compatibilidade futura). |
| **Voto chega antes do candidato** | O `result-service` cria a projeção do candidato a partir do próprio evento de voto. |
| **Reprocessamento** | `POST /api/v1/eventos/republicar` republica eleições, candidatos e votos. Como votos têm `eventId` determinístico e o resto é *upsert*, repetir é seguro. Também roda ao iniciar o `voting-service` (`app.messaging.republish-on-startup`), o que sincroniza consumidores novos e os dados de demonstração. |
| **Mensagens na DLQ** | Inspecione (e reenvie) pelo painel do RabbitMQ: <http://localhost:15672> (`guest`/`guest`). |

> Observação: erros de formato também passam pelo retry (3 tentativas) antes de chegar à DLQ; é aceitável no
> escopo do trabalho e aparece nas "Limitações".

### 9.7 Vantagens e desvantagens observadas

**Vantagens:** desacoplamento (o `voting-service` nem sabe quem consome); processamento assíncrono (registrar
voto não espera a atualização do resultado nem da auditoria); vários consumidores para o mesmo evento; resiliência
(se o `result-service` cair, os eventos ficam na fila e são processados quando ele volta); escalabilidade
(consumidores escalam separadamente do serviço de votação); novos consumidores sem alterar o publicador.

**Desvantagens:** mais complexidade (broker, filas, contratos, três serviços e três bancos); depuração mais difícil
(o caminho de um voto atravessa processos); **consistência eventual** (o resultado no `result-service` pode estar
alguns instantes atrás; por isso a tela permite comparar com a consulta direta); necessidade de tratar mensagens
duplicadas (idempotência); necessidade de observabilidade (Branch 3); maior custo operacional.

### 9.8 Limitações conhecidas (assumidas de propósito)

* **Dual write:** a gravação no banco e a publicação no broker não são atômicas. Se o RabbitMQ estiver fora no
  instante após o commit, o evento não é enviado (o voto é gravado e o erro é apenas logado). A recuperação é a
  republicação; a solução completa seria **Transactional Outbox**.
* Alterar apenas o **nome** de uma eleição (sem mudar o status) não gera evento; o nome só se atualiza na
  projeção quando há um evento de candidato/eleição posterior.
* Erros de formato passam pelo retry antes da DLQ.
* Nenhum dado foi criado por script de migração (`ddl-auto=update`), como na Branch 1.

### 9.9 Como executar a Branch 2

Pré-requisitos: JDK 21, Maven, Node 20+, **PostgreSQL** e **RabbitMQ**.

```bash
# 1) RabbitMQ (uma opção, sem arquivos no repositório; a orquestração completa chega na Branch 3)
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management

# 2) Bancos (no mesmo PostgreSQL usado na Branch 1)
createdb -U postgres resultados
createdb -U postgres auditoria      # "votacao" já existe

# 3) Serviços (um terminal cada)
cd backend        && mvn spring-boot:run     # voting-service  :8080
cd result-service && mvn spring-boot:run     # result-service  :8081
cd audit-service  && mvn spring-boot:run     # audit-service   :8082

# 4) Frontend
cd frontend && npm install && npm run dev    # http://localhost:5173
```

Configuração por variáveis de ambiente (valores padrão apenas para desenvolvimento local):
`DATABASE_URL`, `RESULT_DATABASE_URL`, `AUDIT_DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`,
`RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`.

Sem PostgreSQL: adicione `-Dspring-boot.run.profiles=h2` em cada serviço. Sem RabbitMQ: use
`-Dspring-boot.run.arguments=--app.messaging.enabled=false` (o `voting-service` funciona como na Branch 1; nada é publicado).

**Endpoints novos**

| Serviço | Endpoints |
|---|---|
| `voting-service` | `POST /api/v1/eventos/republicar` (reprocessamento) |
| `result-service` | `GET /result-api/v1/resultados` (`?eleicaoId=&cargo=&estado=`) · `/resultados/candidatos/{id}` · `/resultados/cargos/{cargo}` · `/resultados/regioes/{uf}` |
| `audit-service` | `GET /audit-api/v1/eventos` (`?tipo=&limite=`) · `GET /audit-api/v1/eventos/resumo` |

### 9.10 Testes da Branch 2

```bash
cd backend        && mvn test
cd result-service && mvn test
cd audit-service  && mvn test
cd frontend       && npm test
```

Os testes **não precisam de RabbitMQ** (mensageria desligada e autoconfiguração excluída); testes com broker real
(Testcontainers) ficam para a Branch 3.

| Módulo | Novos testes |
|---|---|
| `voting-service` | `DomainEventPublisherTest` (routing keys, envelope, `eventId` determinístico), `RabbitEventRelayTest` (envio e falha do broker sem afetar o voto), `EventRepublisherTest`, `EleicaoServiceTest` e `CandidatoServiceTest` (quando eventos são/não são publicados); `VotoServiceTest` agora verifica o evento |
| `result-service` | `ResultEventHandlerTest` (contagem, **idempotência**, upsert, remoção, versão inválida, payload inválido, tipo desconhecido), `ResultEventListenerTest` (JSON inválido → DLQ, tolerância a campos novos), `ResultadoIntegrationTest` (eventos → projeção → API) |
| `audit-service` | `AuditEventHandlerTest`, `AuditEventListenerTest`, `AuditoriaIntegrationTest` |
| `frontend` | Fonte dos resultados (eventos × direto), tela de Auditoria, base de URL do cliente (29 testes no total) |

### 9.11 Roteiro de demonstração

1. Abrir o painel do RabbitMQ (`:15672`) → *Exchanges* → `votacao.events` → aba *Bindings* (mostra as duas filas).
2. Em **Votação**, registrar um voto. Em **Auditoria**, ver `VotoRegistrado` chegando; em **Resultado**
   (fonte *Result Service*), ver a contagem atualizada.
3. Alternar a fonte para *Voting Service* e comparar (mesmo total → consistência eventual convergiu).
4. **Resiliência:** parar o `result-service`, registrar 2 votos (a votação segue funcionando), ver 2 mensagens
   acumuladas em `result.queue` no painel, subir o `result-service` de novo e ver as filas esvaziarem.
5. **Mensagem inválida:** no painel, *Exchanges → votacao.events → Publish message* com routing key
   `voto.registrado` e payload `isto não é json`; após as tentativas, a mensagem aparece em `result.dlq` e `audit.dlq`.
6. **Duplicata:** chamar `POST /api/v1/eventos/republicar` duas vezes e mostrar que o resultado **não** muda.

---

## 10. Próxima etapa

* **Branch 3:** Dockerfiles e Docker Compose (todos os serviços, PostgreSQL, RabbitMQ), manifests Kubernetes,
  Actuator/Prometheus/Grafana, logs padronizados com `traceId`/`eventId`, tracing, GitHub Actions,
  Testcontainers e testes E2E.
