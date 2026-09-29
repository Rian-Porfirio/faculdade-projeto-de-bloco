# Sistema de Votação Evolutivo

Simulação acadêmica de urna/sistema de votação, desenvolvida em **três etapas**, cada uma em sua própria branch e
sempre em estado executável:

```text
main
├── TP3       → REST + Spring Boot + PostgreSQL + React + testes
├── TP4       → TP3 + RabbitMQ / arquitetura orientada a eventos
└── TP5       → TP4 + Docker, Kubernetes, observabilidade e CI/CD          (esta versão)
```

> **Você está no TP5.** Tudo do TP3 e do TP4 continua funcionando; esta etapa adiciona Docker, Kubernetes,
> observabilidade, logs padronizados, CI/CD e testes de integração/E2E (seção 10). Nenhuma funcionalidade das
> etapas anteriores foi removida.

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

RabbitMQ e eventos (TP4); Docker, Kubernetes, observabilidade, CI/CD e testes E2E (TP5); autenticação;
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
# (TP4: também result-service e audit-service, ver seção 9.10)
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

## 8. TP3 em uma frase

Frontend React → API REST (Spring Boot) → PostgreSQL. Tudo síncrono, em um único serviço. As seções 1 a 7
descrevem essa base, que o TP4 **não recria**: apenas evolui.

---

## 9. TP4 — arquitetura orientada a eventos (RabbitMQ)

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
duplicadas (idempotência); necessidade de observabilidade (TP5); maior custo operacional.

### 9.8 Limitações conhecidas (assumidas de propósito)

* **Dual write:** a gravação no banco e a publicação no broker não são atômicas. Se o RabbitMQ estiver fora no
  instante após o commit, o evento não é enviado (o voto é gravado e o erro é apenas logado). A recuperação é a
  republicação; a solução completa seria **Transactional Outbox**.
* Alterar apenas o **nome** de uma eleição (sem mudar o status) não gera evento; o nome só se atualiza na
  projeção quando há um evento de candidato/eleição posterior.
* Erros de formato passam pelo retry antes da DLQ.
* Nenhum dado foi criado por script de migração (`ddl-auto=update`), como no TP3.

### 9.9 Como executar o TP4

Pré-requisitos: JDK 21, Maven, Node 20+, **PostgreSQL** e **RabbitMQ**.

```bash
# 1) RabbitMQ (uma opção, sem arquivos no repositório; a orquestração completa chega no TP5)
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management

# 2) Bancos (no mesmo PostgreSQL usado no TP3)
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
`-Dspring-boot.run.arguments=--app.messaging.enabled=false` (o `voting-service` funciona como no TP3; nada é publicado).

**Endpoints novos**

| Serviço | Endpoints |
|---|---|
| `voting-service` | `POST /api/v1/eventos/republicar` (reprocessamento) |
| `result-service` | `GET /result-api/v1/resultados` (`?eleicaoId=&cargo=&estado=`) · `/resultados/candidatos/{id}` · `/resultados/cargos/{cargo}` · `/resultados/regioes/{uf}` |
| `audit-service` | `GET /audit-api/v1/eventos` (`?tipo=&limite=`) · `GET /audit-api/v1/eventos/resumo` |

### 9.10 Testes do TP4

```bash
cd backend        && mvn test
cd result-service && mvn test
cd audit-service  && mvn test
cd frontend       && npm test
```

Os testes **não precisam de RabbitMQ** (mensageria desligada e autoconfiguração excluída); testes com broker real
(Testcontainers) ficam para o TP5.

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

## 10. TP5 — Docker, Kubernetes, observabilidade e CI/CD

### 10.1 Visão geral da infraestrutura

```text
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                    Docker Compose / Kubernetes                        │
│                                                                                        │
│   ┌──────────┐     ┌────────────────┐     ┌─────────────────┐     ┌────────────────┐ │
│   │ frontend │────▶│ voting-service │────▶│  PostgreSQL      │     │   RabbitMQ     │ │
│   │ (nginx)  │     │     :8080      │     │ votacao/         │     │  (votacao.     │ │
│   └──────────┘     └───────┬────────┘     │ resultados/      │◀───▶│   events)      │ │
│        │                   │ eventos      │ auditoria        │     └───────┬────────┘ │
│        │           ┌───────┴────────┐     └─────────────────┘             │          │
│        ├──────────▶│ result-service │◀────────────────────────────────────┤          │
│        │           │     :8081      │                                     │          │
│        │           └────────────────┘                                     │          │
│        │           ┌────────────────┐                                     │          │
│        └──────────▶│ audit-service  │◀────────────────────────────────────┘          │
│                     │     :8082      │                                                │
│                     └────────────────┘                                                │
│                                                                                        │
│   ┌───────────┐   ┌────────────┐   ┌─────────┐                                        │
│   │ Prometheus│◀──│  Grafana   │   │ Jaeger  │◀── traces (OTLP) dos 3 serviços Java    │
│   └─────┬─────┘   └────────────┘   └─────────┘                                        │
│         └─ scrape /actuator/prometheus dos 3 serviços a cada 10s                      │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

O frontend (nginx) faz proxy reverso de `/api`, `/result-api` e `/audit-api` para os três serviços — o
mesmo `nginx.conf` funciona no Docker Compose e no Kubernetes porque os nomes DNS (`voting-service`,
`result-service`, `audit-service`) são iguais nos dois ambientes (nome do serviço).

### 10.2 Containerização (Docker)

* **`backend/Dockerfile`, `result-service/Dockerfile`, `audit-service/Dockerfile`**: multi-stage
  (`maven:3.9-eclipse-temurin-21` para build → `eclipse-temurin:21-jre-jammy` para execução), usuário
  não-root, `HEALTHCHECK` batendo em `/actuator/health`.
* **`frontend/Dockerfile`**: multi-stage (`node:20-alpine` para build → `nginx:1.27-alpine` para servir os
  estáticos e fazer o proxy reverso, `nginx.conf`).
* **`docker-compose.yml`** (raiz do repositório): sobe tudo — PostgreSQL (com 3 bancos, criados por
  `infra/postgres/init-databases.sh`), RabbitMQ, os 3 serviços, o frontend, Prometheus, Grafana e Jaeger.
  Cada serviço Java tem `HEALTHCHECK`, e os dependentes usam `depends_on: condition: service_healthy`.

```bash
docker compose up --build     # primeira vez (ou após alterar código)
docker compose up -d          # subidas seguintes
docker compose down -v        # para tudo e apaga os volumes (bancos, métricas)
```

Sistema disponível em <http://localhost:8090>. Painéis: RabbitMQ `:15672` (guest/guest), Prometheus `:9090`,
Grafana `:3000` (admin/admin, dashboard "Sistema de Votação" já provisionado), Jaeger `:16686`.

### 10.3 Kubernetes

Manifests em `k8s/` (aplicar nesta ordem, ou tudo de uma vez com `kubectl apply -f k8s/`):

```text
k8s/
├── namespace.yaml            # namespace "votacao"
├── configmap.yaml            # URLs, hosts (não sensível)
├── secrets.yaml              # credenciais de EXEMPLO (ver aviso no arquivo)
├── postgres.yaml             # Deployment + PVC + Service (1 réplica: tem estado)
├── rabbitmq.yaml             # Deployment + Service
├── voting-deployment.yaml / voting-service.yaml     (2 réplicas)
├── result-deployment.yaml  / result-service.yaml    (2 réplicas)
├── audit-deployment.yaml   / audit-service.yaml     (1 réplica)
├── frontend-deployment.yaml/ frontend-service.yaml  (2 réplicas)
├── jaeger.yaml / prometheus.yaml / grafana.yaml      # observabilidade dentro do cluster
└── ingress.yaml               # expõe o frontend (que já faz proxy para os demais)
```

```bash
# 1) construir as imagens localmente e carregá-las no cluster de testes (kind neste exemplo)
docker compose build
kind load docker-image votacao/voting-service:latest votacao/result-service:latest   votacao/audit-service:latest votacao/frontend:latest

# 2) aplicar os manifests
kubectl apply -f k8s/

# 3) acompanhar
kubectl get pods -n votacao -w
```

Com um Ingress Controller instalado (`minikube addons enable ingress` ou o do `kind`) e
`127.0.0.1 votacao.local` no `/etc/hosts`, o sistema fica em <http://votacao.local>.

**Escalabilidade horizontal** (demonstração pedida no enunciado):

```bash
kubectl scale deployment voting-service --replicas=3 -n votacao
kubectl get pods -n votacao -l app=voting-service
```

Como o `voting-service` não guarda estado em memória (estado vive no PostgreSQL) e o RabbitMQ distribui as
mensagens entre consumidores da mesma fila, escalar não duplica votos nem eventos: cada réplica processa
requisições/mensagens diferentes.

`readinessProbe`/`livenessProbe` usam `/actuator/health/readiness` e `/actuator/health/liveness`
(Spring Boot Actuator, habilitado na seção 10.4), então o Kubernetes só envia tráfego a pods realmente
prontos (ex.: que já conectaram no PostgreSQL).

### 10.4 Configuração (sem segredos no código)

Nenhuma senha está no código-fonte ou nas imagens. Tudo vem de variáveis de ambiente:

| Variável | Uso |
|---|---|
| `DATABASE_URL`, `RESULT_DATABASE_URL`, `AUDIT_DATABASE_URL` | URL JDBC de cada serviço (não sensível → ConfigMap) |
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | credenciais do PostgreSQL (sensível → Secret) |
| `RABBITMQ_HOST`, `RABBITMQ_PORT` | endereço do broker (ConfigMap) |
| `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | credenciais do broker (Secret) |
| `OTLP_TRACING_ENDPOINT` | endpoint do Jaeger para envio de traces |
| `APP_CORS_ALLOWED_ORIGINS` | origens permitidas (não sensível) |

No Docker Compose isso vem do `docker-compose.yml`/`.env` (veja `.env.example`); no Kubernetes, de
`configmap.yaml` e `secrets.yaml`. O profile Spring `dev` (`application-dev.properties`, todos os serviços)
só existe para rodar **local, fora de container**, com valores de desenvolvimento
(`mvn spring-boot:run -Dspring-boot.run.profiles=dev`) — nunca é usado em Docker/Kubernetes.

> `k8s/secrets.yaml` contém valores de exemplo em texto puro **apenas para o trabalho ficar executável na
> apresentação**. O arquivo documenta, em comentário, como isso seria feito de verdade (`kubectl create
> secret`, um cofre externo, ou Sealed Secrets/SOPS) — nunca versionar segredos reais em Git.

### 10.5 Observabilidade

* **Métricas técnicas**: Spring Boot Actuator + Micrometer expõem `/actuator/prometheus` em cada serviço
  (latência HTTP, uso de memória/CPU, conexões do pool, etc.), coletado pelo Prometheus a cada 10s
  (`infra/prometheus/prometheus.yml` no Compose, `ConfigMap prometheus-config` no Kubernetes).
* **Métricas de negócio** (código, não vêm de graça): contadores Micrometer criados a mão —
  `votacao_eventos_dominio_total{tipo=...}` (quantos eventos de cada tipo o `voting-service` confirmou, ex.:
  votos registrados), `votacao_eventos_publicados_total{resultado=sucesso|falha}` (publicação no RabbitMQ),
  `votacao_eventos_processados_total{tipo,resultado=aplicado|duplicado|ignorado|rejeitado}` (consumo no
  `result-service`) e `votacao_eventos_auditados_total{resultado=registrado|duplicado|rejeitado}`
  (`audit-service`).
* **Grafana**: dashboard "Sistema de Votação" já provisionado (`infra/grafana/dashboards`), com os painéis
  acima prontos (votos/min, eventos processados, latência p95, instâncias saudáveis).
* **Tracing distribuído**: Micrometer Tracing + OpenTelemetry exportam para o Jaeger via OTLP/HTTP.
  `spring.rabbitmq.template.observation-enabled` e `...listener.simple.observation-enabled=true` propagam o
  **mesmo traceId através do RabbitMQ**: no Jaeger, o span do `POST /api/v1/votos` e o span do consumo em
  `result-service`/`audit-service` aparecem no mesmo trace — a evidência visual de que o fluxo é
  `Request → Voting Service → RabbitMQ → Result Service`.

```text
Request (traceId=abc123)
   │
   ▼
Voting Service  ── span "POST /api/v1/votos" (traceId=abc123)
   │
   ▼
RabbitMQ  ── traceId propagado nos headers da mensagem
   │
   ▼
Result Service  ── span "voto.registrado process" (traceId=abc123)   ← mesmo traceId no Jaeger
```

### 10.6 Logs

Todos os serviços usam o mesmo padrão (`logging.pattern.console`, em cada `application.properties`):

```text
2026-09-27T20:30:01.123-03:00 level=INFO  service=voting-service traceId=ab12cd34ef56 eventId=5b0c8b7e-... logger=b.e.v.m.RabbitEventRelay - Evento publicado tipo=VotoRegistrado routingKey=voto.registrado
```

`timestamp`, `level` e `logger` vêm do Logback; `service` vem de `spring.application.name`; `traceId` é
preenchido automaticamente pelo Micrometer Tracing; `eventId` é colocado manualmente no MDC
(`RabbitEventRelay`, `ResultEventHandler`, `AuditEventHandler`) ao redor do processamento de cada evento, e
removido ao final (try-with-resources) para não vazar entre requisições. Nenhum dado sensível (senha,
identificador de eleitor) é logado.

### 10.7 CI/CD (GitHub Actions)

`.github/workflows/ci.yml`, executa em todo push/PR:

```text
push/PR
   │
   ├─► backend-tests (matriz: voting/result/audit) → mvn test → mvn verify (Testcontainers) → mvn package
   ├─► frontend                                    → tsc → vitest → vite build
   ├─► validate-k8s                                → kubeconform valida os manifests em k8s/
   └─► e2e (após os três acima)                     → docker compose up → Playwright → docker compose down
          │
          ▼ (somente push em main/TP5)
      build-and-push → build das 4 imagens Docker → publica no GHCR (GitHub Container Registry)
```

Pipeline pensado para dar sinal rápido: os testes unitários/slice (`mvn test`, sem Testcontainers) rodam
primeiro; os mais lentos (integração com containers reais e E2E) rodam depois. O build e a publicação das
imagens só acontecem depois de tudo passar, e só nas branches principais — evitando publicar imagem de um
branch de experimento.

### 10.8 Testes do TP5

```bash
cd backend        && mvn verify   # test (unit/slice) + *IT (Testcontainers: precisa de Docker)
cd result-service && mvn verify
cd audit-service  && mvn verify
cd frontend       && npm test
cd e2e            && npm test     # requer `docker compose up` rodando (ver e2e/README.md)
```

| Nível | Onde | O que verifica |
|---|---|---|
| **Unitário** | `*Test.java` em todos os módulos (TP3 e TP4) | regras isoladas, com mocks |
| **Integração "API + banco"** | `backend/.../VotingServicePostgresIT.java` | fluxo HTTP completo contra **PostgreSQL real** (Testcontainers), não H2 |
| **Integração "API + RabbitMQ"** | `backend/.../VotingServiceRabbitMQIT.java` | registrar um voto pela API publica de fato o evento num **RabbitMQ real** |
| **Integração "Consumer + banco"** | `result-service/.../ResultConsumerIT.java`, `audit-service/.../AuditConsumerIT.java` | evento publicado num broker real chega ao `@RabbitListener` e é persistido num **PostgreSQL real** |
| **End-to-End** | `e2e/tests/votacao.spec.ts` (Playwright) | navegador real contra o sistema completo: votar → comprovante → resultado atualizado (assíncrono) → bloqueio de 2º voto → evento aparece na auditoria → CRUD de eleitor |

Os testes `*IT` (sufixo IT) rodam com `mvn verify`/`mvn failsafe:integration-test`, não com `mvn test` —
mantém o ciclo de desenvolvimento rápido (só os unitários) e isola os testes que exigem Docker.

### 10.9 O que ficou de fora (propositalmente)

Conforme a seção "O que não fazer" do enunciado: sem autenticação/OAuth/Keycloak, sem múltiplos bancos além
do necessário (1 banco por serviço, já existente desde o TP4), sem operador de banco de dados
(Patroni/CloudNativePG) — o PostgreSQL roda como um único Deployment simples, adequado ao escopo acadêmico e
documentado como limitação (não teria alta disponibilidade em produção real).

---

## 11. Diagramas da evolução

```text
TP3: comunicação síncrona
  React → REST → Spring Boot → PostgreSQL

TP4: + arquitetura orientada a eventos
  Voting Service → RabbitMQ → Result Service / Audit Service (consumers)

TP5: + infraestrutura e operação
                    ┌──────────────────────┐
                    │      Kubernetes       │
                    │  (ou Docker Compose)  │
  Frontend ────────▶│  Voting  · Result     │
                    │  Audit   · RabbitMQ   │
                    │  PostgreSQL (x3 db)   │
                    └───────────┬───────────┘
                                │
                         Observabilidade
                                │
                    Prometheus · Grafana · Jaeger
```

---

## 12. Resumo executivo

| | TP3 | TP4 | TP5 |
|---|---|---|---|
| Comunicação | REST síncrono | REST + eventos assíncronos | idem TP4 |
| Serviços | 1 (voting-service) | 3 (voting, result, audit) | idem TP4, containerizados |
| Infraestrutura | `mvn`/`npm` locais | idem TP3 | Docker, Kubernetes |
| Observabilidade | — | logs simples | métricas, tracing, dashboards, logs padronizados |
| Entrega | manual | manual | CI/CD (GitHub Actions), imagens publicadas |
| Testes | unitário, slice | + verificação de publicação de evento (mock) | + Testcontainers (broker/banco reais) e E2E |