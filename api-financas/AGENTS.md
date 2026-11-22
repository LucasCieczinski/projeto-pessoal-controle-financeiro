# Regras do Backend Java — `api-financas` (Java 21 + Spring Boot 4.1)

Este módulo implementa a API REST `/api/v1/*` seguindo **Clean Architecture + Arquitetura Hexagonal (Ports & Adapters)** ([ADR-001](../docs/adr/ADR-001-clean-architecture-e-ports-and-adapters.md)).

## 1. As 4 Camadas Canônicas (`peres.lucas.apifinanceira`)

A raiz `src/main/java/peres/lucas/apifinanceira` deve conter **exclusivamente** os 4 pacotes abaixo:

1. **`domain/` (`model`, `port`, `exception`) — 100% Java Puro**
   - **Imports proibidos:** `org.springframework.*`, `jakarta.persistence.*`, `org.hibernate.*`, `com.fasterxml.jackson.*`, `lombok.*` e qualquer pacote de `application`, `adapter` ou `infrastructure`.
   - **Anotações proibidas:** classes e interfaces em `domain.model` e `domain.port` não devem ter **nenhuma** anotação de framework (`@Entity`, `@Component`, `@Service`, etc.).
   - **Modelos ricos:** higienizam strings (`trim()`), validam nulidade e invariantes de domínio no construtor/métodos de fábrica e mutação (ex.: `Movement.nova(...)`, `Movement.atualizarDados(...)`). Campos `id`, `usuarioId` e `dataCadastro` são imutáveis (`final`). Valores monetários usam sempre `BigDecimal`.
   - **Portas (`*RepositoryPort`, `*Port`):** interfaces que usam apenas tipos Java padrão ou tipos de `domain.model` (nunca `Pageable`, `Page`, `*Entity` ou DTOs).
   - **Exceções de domínio:** estendem `RuntimeException` (ex.: `MovementNotFoundException`, `InvalidRequestException`, `UserNotFoundException`).

2. **`application/` (`usecase`, `dto`)**
   - **UseCases:** classes anotadas com `@Service`, com sufixo obrigatório **`UseCase`** (ex.: `MovementUseCase`, `IncomeUseCase`). Métodos de leitura usam `@Transactional(readOnly = true)` e mutações usam `@Transactional`.
   - **Dependências permitidas:** apenas portas de `domain.port`, modelos/exceções de `domain` e DTOs de `application.dto`. **Nunca** injetar `*JpaRepository`, `*Entity`, `HttpServletRequest` ou `SecurityContextHolder` diretamente se o `Principal`/`email` puder ser passado pelo Controller.
   - **Isolamento de usuário:** toda operação escopada ao usuário resolve `User` via `UserRepositoryPort.findByEmail(email)` e consulta/muta recursos filtrando por `user.getId()` (ex.: `findByIdAndUserId(id, userId)`).
   - **DTOs:** `record`s Java imutáveis com sufixo `Dto` (`*RequestDto`, `*ResponseDto`), utilizando Bean Validation (`@NotBlank`, `@NotNull`, `@DecimalMin`, `@Digits`, `@Min`, `@Max`, `@Size`, etc.) nos records de entrada.

3. **`adapter/` (`adapter.web.controller`, `adapter.web.exception`)**
   - **Controllers REST:** usam `@RestController`, `@RequestMapping` apontando para constantes de `infrastructure.config.ApiPaths`, `@Tag` e `@SecurityRequirement(name = "cookieAuth")` para OpenAPI.
   - **Identidade autenticada:** recebem `java.security.Principal principal` e repassam `principal.getName()` ao `*UseCase`. Nunca aceitam `usuarioId` vindo do payload JSON ou query param para identificar o dono do recurso.
   - **Tratamento de Erros (`GlobalExceptionHandler`):** toda nova `*NotFoundException` ou exceção de negócio deve ser mapeada no `GlobalExceptionHandler` retornando a classe padronizada `ErrorMessage` (`status`, `error`, `path`, `mensagem`, `campos`). Note que `ErrorMessage` é uma `class` com getters (`getStatus()`, `getError()`, `getPath()`, `getMensagem()`, `getCampos()`).

4. **`infrastructure/` (`config`, `persistence.*`, `security`)**
   - **`config/ApiPaths.java`:** centraliza todas as rotas `/api/v1/*` (zero magic strings em Controllers ou `SecurityConfig`).
   - **`persistence/entity/*Entity.java`:** entidades JPA (`@Entity`, `@Table(name = "tb_...")`, `@Getter`, `@Setter` do Lombok). Associam o dono com `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false) private UserEntity user;`.
   - **`persistence/repository/*JpaRepository.java`:** interfaces `JpaRepository<*Entity, UUID>` com queries escopadas ao usuário (ex.: `findByIdAndUser_Id(UUID id, UUID userId)`).
   - **`persistence/adapter/*PersistenceAdapter.java`:** classes `@Component` que implementam `*RepositoryPort`, obtêm a referência do usuário via `userRepository.getReferenceById(model.getUsuarioId())` ao persistir e convertem bidirecionalmente entre `*Entity` e `domain.model.*`.
   - **Segurança:** cookies `HttpOnly` (`atona_access`, `atona_refresh`), CSRF SPA (`CookieCsrfTokenRepository` + `X-XSRF-TOKEN`), Rate Limiting (`RateLimitPort`), Cache Caffeine (`CacheConfig`) e `CorrelationIdFilter` (MDC).

## 2. Regra Crítica: Fitness Function (`CleanArchitectureTest`)

Sempre que adicionar ou renomear classes em `domain.model`, `domain.port` ou `application.usecase`, **você deve atualizar** as listas `domainClasses` e `applicationUseCases` em:
`src/test/java/peres/lucas/apifinanceira/CleanArchitectureTest.java`.

## 3. Migrations & Padrão de Testes

- **Flyway:** migrations em `src/main/resources/db/migration/`. Os testes rodam com H2 em memória no profile `test` (`@ActiveProfiles("test")`), validando a sintaxe compatível com PostgreSQL/H2.
- **Cobertura de Testes:**
  - Testes unitários puros para regras/invariantes de `domain.model` e lógica de `UseCase`.
  - Testes de integração `MockMvc` (`@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, `@Transactional`) validando: `401` sem sessão, `403` sem `X-XSRF-TOKEN` em mutações, `400` com `$.campos.*` em entrada inválida, isolamento entre dois usuários (`ana` vs `bia` retornando `404` no acesso cruzado) e caminho feliz completo (`201`, `200`, `204`).
- **Quality Gate Obrigatório (JaCoCo >= 80%):**
  - Rodar suíte de testes: `./mvnw test` (ou `.\mvnw.cmd test` no Windows)
  - Validar cobertura mínima de 80% de linhas (`jacoco:check`): `./mvnw verify` (ou `.\mvnw.cmd verify`)
