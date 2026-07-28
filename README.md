# Documentação — Camada de Persistência (Sistema de Biblioteca)

Segunda etapa do Projeto Integrado da disciplina Engenharia de Softwares Escaláveis.

Nesta etapa foi implementada a camada de persistência da aplicação usando JPA/Hibernate
integrado ao Spring Data JPA, com operações CRUD, controle de integridade, histórico de
alterações (auditoria) e testes automatizados.

## Suposições assumidas

O domínio da aplicação monolítica da primeira etapa não acompanhou o material desta etapa.
Por isso foi assumido o domínio de um **Sistema de Gerenciamento de Biblioteca**, que é
adequado ao escopo da disciplina e permite exercitar todos os tipos de relacionamento pedidos.

Também foi assumido o uso do banco **H2 em memória** para desenvolvimento e testes, mantendo
o projeto executável sem depender de um servidor externo. Como todo o mapeamento é feito por
JPA, a troca para PostgreSQL ou MySQL exige apenas alterar as propriedades de datasource e a
dependência do driver.

## Como executar

- Rodar a aplicação: `mvn spring-boot:run`
- Rodar os testes: `mvn test`
- Console do banco (com a aplicação em execução): `http://localhost:8080/h2-console`
  (JDBC URL `jdbc:h2:mem:biblioteca`, usuário `sa`, sem senha).

## Tecnologias

- Java 17
- Spring Boot 3.3
- Spring Data JPA
- Hibernate (JPA) e Hibernate Envers (auditoria)
- Banco H2
- JUnit 5, Spring Boot Test, Mockito

---

## 1. Arquitetura

A camada de persistência foi organizada em pacotes com responsabilidades bem definidas,
seguindo a separação em camadas comum em aplicações Spring:

- `model` — entidades JPA e objetos de valor (`@Embeddable`). Representam o domínio e o
  mapeamento objeto-relacional.
- `repository` — interfaces do Spring Data JPA. Responsáveis pelo acesso aos dados
  (consultas, gravação, remoção).
- `service` — regras de negócio e controle transacional. É a camada que coordena os
  repositórios, garante a integridade e valida as regras antes de persistir.
- `service.exception` — exceções de domínio usadas pelos serviços.

O fluxo de responsabilidade é: **Service → Repository → Entidade**. As entidades nunca são
manipuladas diretamente por fora dos serviços, o que mantém o domínio isolado e evita
duplicidade de regras.

### Responsabilidades das classes principais

- `BibliotecaApplication` — classe de inicialização do Spring Boot.
- Entidades (`Autor`, `Categoria`, `Livro`, `Membro`, `Emprestimo`, `Multa`) — mapeiam as
  tabelas do banco e seus relacionamentos.
- `Endereco` — objeto de valor embutido em `Membro`.
- `StatusEmprestimo` — enum com os estados possíveis de um empréstimo.
- Repositórios — abstraem o acesso a cada entidade.
- `LivroService`, `MembroService` — CRUD e regras de integridade (unicidade).
- `EmprestimoService` — regras de negócio do empréstimo e da devolução (baixa de estoque,
  limite de empréstimos, geração de multa por atraso).
- `HistoricoService` — consulta do histórico de alterações via Hibernate Envers.

---

## 2. Modelo de Dados

### Entidades e atributos

**Autor** — `id`, `nome`, `nacionalidade`.

**Categoria** — `id`, `nome` (único).

**Livro** — `id`, `titulo`, `isbn` (único), `anoPublicacao`, `exemplaresDisponiveis`,
`categoria`, `autores`.

**Membro** — `id`, `nome`, `email` (único), `dataCadastro`, `endereco` (embutido),
`emprestimos`.

**Emprestimo** — `id`, `livro`, `membro`, `dataEmprestimo`, `dataDevolucaoPrevista`,
`dataDevolucaoReal`, `status`, `multa`.

**Multa** — `id`, `emprestimo`, `valor`, `paga`, `dataGeracao`.

**Endereco** (`@Embeddable`) — `logradouro`, `cidade`, `estado`, `cep`.

### Relacionamentos

- `Livro` **N:1** `Categoria` — `@ManyToOne` com `@JoinColumn(categoria_id)`. Um livro
  pertence a uma categoria; uma categoria tem vários livros.
- `Livro` **N:N** `Autor` — `@ManyToMany` com `@JoinTable(livro_autor)`. Um livro pode ter
  vários autores e um autor pode ter vários livros.
- `Emprestimo` **N:1** `Livro` e `Emprestimo` **N:1** `Membro` — `@ManyToOne`. Cada
  empréstimo registra um livro e um membro.
- `Membro` **1:N** `Emprestimo` — `@OneToMany(mappedBy = "membro")`, lado inverso do
  relacionamento acima, permitindo navegar dos membros para seus empréstimos.
- `Emprestimo` **1:1** `Multa` — `@OneToOne`. Uma multa está associada a um único empréstimo
  (gerada apenas em devoluções atrasadas). O lado dono é `Multa`, que possui a chave
  estrangeira `emprestimo_id`.
- `Membro` possui um `Endereco` embutido (`@Embedded` / `@Embeddable`), cujos campos ficam
  na própria tabela de `membro`.
- `Emprestimo.status` usa `@Enumerated(EnumType.STRING)`, gravando o texto do enum para
  facilitar a leitura direta no banco.

### Decisões de modelagem

- **`EnumType.STRING`** foi escolhido em vez de `ORDINAL` porque preserva o significado no
  banco e evita quebra caso a ordem dos valores do enum mude.
- **`exemplaresDisponiveis`** foi mantido no próprio `Livro` como um contador simples,
  suficiente para o escopo e para controlar a disponibilidade nos empréstimos.
- **Integridade referencial** garantida pelas chaves estrangeiras (`@JoinColumn`) e por
  restrições de unicidade (`isbn`, `email`, `categoria.nome`) declaradas nas colunas.
- **`GenerationType.IDENTITY`** para os identificadores, compatível com o auto-incremento
  do H2 e de bancos relacionais comuns.

### Integridade e performance

A **integridade** dos dados é assegurada em duas frentes: no banco, por chaves estrangeiras
(`@JoinColumn`) e restrições de unicidade (`isbn`, `email`, `categoria.nome`, `emprestimo_id`
na multa); e na aplicação, pelas validações da camada de serviço (verificação de ISBN e
e-mail já existentes, controle de estoque antes de emprestar, limite de empréstimos ativos
por membro). Toda operação que grava passa por métodos anotados com `@Transactional`, o que
garante atomicidade — se uma regra falhar, nada é persistido.

Do lado da **performance**: os relacionamentos usam `FetchType.LAZY` para evitar carregar
dados desnecessários e o problema de N+1 em cascata; as consultas de leitura usam
`@Transactional(readOnly = true)`, permitindo que o provedor otimize a sessão; e o acesso
por atributos únicos (ISBN, e-mail) é naturalmente indexado pelas restrições `unique`.
Índices adicionais nas colunas mais consultadas estão listados em melhorias futuras.

---

## 3. Repositórios

Todos os repositórios estendem `JpaRepository`, herdando as operações CRUD e de paginação.
As consultas foram criadas preferencialmente por **derivação pelo nome do método**, e uma
consulta customizada com `@Query` foi usada apenas quando ficou mais claro que a versão
derivada.

- `LivroRepository`
  - `findByIsbn`, `existsByIsbn` — busca e verificação por ISBN.
  - `findByTituloContainingIgnoreCase` — busca parcial por título.
  - `findByCategoriaNome` — navega pelo relacionamento até `categoria.nome`.
  - `buscarDisponiveis` — `@Query` que retorna livros com exemplares em estoque.
- `MembroRepository` — `findByEmail`, `existsByEmail`.
- `EmprestimoRepository`
  - `findByMembroId`, `findByStatus`.
  - `findByStatusAndDataDevolucaoPrevistaBefore` — localiza empréstimos vencidos.
  - `contarPorMembroEStatus` — `@Query` para contar empréstimos ativos de um membro.
- `CategoriaRepository`, `AutorRepository`, `MultaRepository` — consultas auxiliares.

### Exemplo de utilização

```java
Livro livro = livroService.cadastrar(novoLivro);
List<Livro> disponiveis = livroRepository.buscarDisponiveis();
Optional<Livro> porIsbn = livroRepository.findByIsbn("978-85-333-0227-3");
```

---

## 4. Histórico de Dados (Auditoria)

### Estratégia utilizada

A auditoria foi implementada com **Hibernate Envers**. As entidades que precisam de histórico
recebem a anotação `@Audited`. A partir disso, o Envers cria automaticamente tabelas de
auditoria (sufixo `_AUD`) e uma tabela de revisões (`REVINFO`), e registra uma nova versão a
cada inserção, atualização ou remoção — dentro do commit da transação.

Essa estratégia foi escolhida porque é a solução padrão do ecossistema JPA/Hibernate para
versionamento de dados: evita criar e manter manualmente tabelas de log, mantém o histórico
completo de cada versão e permite consultar o estado de uma entidade em qualquer revisão.

O lado inverso `Membro.emprestimos` recebe `@NotAudited`, pois o próprio `Emprestimo` já é
auditado e auditar a coleção inversa geraria revisões redundantes.

### Consulta do histórico

O `HistoricoService` encapsula o `AuditReader` do Envers e oferece:

- `listarRevisoes(Entidade.class, id)` — números das revisões de um registro.
- `buscarVersao(Entidade.class, id, revisao)` — estado da entidade em uma revisão específica.
- `listarHistorico(Entidade.class, id)` — todas as versões de um registro.

```java
List<Number> revisoes = historicoService.listarRevisoes(Livro.class, livroId);
Livro versaoInicial = historicoService.buscarVersao(Livro.class, livroId, revisoes.get(0));
```

---

## 5. Testes

Os testes cobrem a persistência, as consultas, as regras de negócio e o histórico.

- **`LivroRepositoryTest`** (`@DataJpaTest`) — persistência, busca por id e por ISBN,
  listagem de disponíveis, busca por categoria, atualização e remoção.
- **`MembroRepositoryTest`** (`@DataJpaTest`) — persistência do `Endereco` embutido e
  consultas por e-mail.
- **`EmprestimoRepositoryTest`** (`@DataJpaTest`) — consultas derivadas por membro, por
  status, por vencimento, e a consulta customizada de contagem.
- **`EmprestimoServiceTest`** (JUnit 5 + Mockito) — regras de negócio isoladas dos
  repositórios: baixa de estoque, bloqueio quando não há exemplares, limite de empréstimos
  ativos, geração de multa em atraso e ausência de multa quando a devolução é no prazo.
- **`AuditoriaEnversTest`** (`@SpringBootTest`) — valida o histórico: cria e altera um livro
  e verifica que as revisões guardam os títulos original e atualizado.

### Justificativa das escolhas

- `@DataJpaTest` carrega apenas a camada JPA com um H2 dedicado, deixando os testes de
  repositório rápidos e focados no banco.
- Mockito foi usado no teste de serviço para validar apenas a lógica de negócio, sem
  acessar o banco, tornando o teste rápido e determinístico.
- O teste de auditoria usa `@SpringBootTest` com `TransactionTemplate` porque o Envers grava
  os registros de histórico no commit da transação. Com transações efetivamente confirmadas,
  o `AuditReader` consegue ler as revisões — o que não aconteceria com o rollback automático
  padrão do `@DataJpaTest`.

---

## 6. Melhorias futuras

- Adicionar uma camada de DTOs para não expor as entidades diretamente.
- Registrar o autor da alteração no histórico (usar `@CreatedBy` / `RevisionEntity`
  customizada) quando houver autenticação.
- Substituir o H2 por PostgreSQL em ambiente de produção e usar migrations (Flyway) em vez
  de `ddl-auto`.
- Criar índices explícitos nas colunas mais consultadas (ISBN, e-mail, status).
- Evoluir o controle de estoque de exemplares para uma entidade própria de exemplar físico,
  caso o domínio passe a exigir rastreio individual.
