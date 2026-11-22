---
name: onda2-vertical-slice
description: >-
  Implementa uma fatia vertical completa (Backend Java Hexagonal + Frontend Angular 22 + Testes Unitários/Integração)
  das funcionalidades da Onda 2 (Income, RecurringExpense, Debt, CreditCard) seguindo o plano-onda-2.md e a Clean Architecture.
---

# Fatia Vertical da Onda 2 (`À Tona`)

Use este runbook sempre que implementar uma nova entidade ou sprint de `docs/plano-onda-2.md` (`Income`, `RecurringExpense`, `Debt`, `CreditCard`).

## Passo 1: Conferir Contrato SQL e Escopo da Sprint

1. Leia as definições de tabela, colunas e `CHECK` constraints em:
   - `api-financas/src/main/resources/db/migration/V1__create_financial_schema.sql`
   - `api-financas/src/main/resources/db/migration/V2__align_financial_domain_rules.sql`
2. Confira os requisitos funcionais e o checklist da sprint correspondente em `docs/plano-onda-2.md`.
3. Não crie migrations novas se a tabela já estiver completa em `V1` e `V2`. Se precisar de nova coluna/constraint, crie uma migration `V4__...sql` compatível com PostgreSQL e H2.

## Passo 2: Camada `domain` (Java 100% Puro)

Em `api-financas/src/main/java/peres/lucas/apifinanceira/domain/`:

1. **Modelo (`domain/model/<Entidade>.java`):**
   - Zero imports de Spring, JPA, Hibernate, Jackson ou Lombok.
   - Campos `id`, `usuarioId` (e timestamps imutáveis quando houver) devem ser `final`.
   - Use `BigDecimal` para todos os valores monetários.
   - Higienize strings com `.trim()` e valide nulidades (`Objects.requireNonNull(...)`) e invariantes de domínio no construtor/método de atualização (ex.: `valor > 0`, `diaVencimento` entre `1` e `31`, `parcelasPagas <= totalParcelas`, transição para `QUITADA`), lançando `InvalidRequestException` para violações de regra de negócio.
   - Forneça método de fábrica estático (ex.: `nova(...)`) e método de mutação controlado (`atualizarDados(...)` / `registrarPagamentoParcela()`).
2. **Exceção (`domain/exception/<Entidade>NotFoundException.java`):**
   - Estende `RuntimeException` com mensagem clara em português (sem vazar dados internos).
3. **Porta (`domain/port/<Entidade>RepositoryPort.java`):**
   - Interface pura recebendo e retornando apenas tipos Java padrão e `domain.model.*`.
   - Inclua método escopado pelo dono: `Optional<T> findByIdAndUserId(UUID id, UUID userId)` e listagens por `UUID userId`.

## Passo 3: Camada `application` (UseCase e DTOs)

Em `api-financas/src/main/java/peres/lucas/apifinanceira/application/`:

1. **DTOs (`application/dto/`):**
   - Crie `record`s imutáveis com sufixo `Dto` (ex.: `IncomeRequestDto`, `IncomeResponseDto`).
   - Adicione anotações de Bean Validation nos `*RequestDto`: `@NotBlank`, `@NotNull`, `@DecimalMin("0.01")` (ou `"0.00"` para limite de cartão), `@Digits(integer = 10, fraction = 2)`, `@Min`, `@Max`, `@Size`.
   - Nunca inclua `usuarioId` nos DTOs de entrada.
2. **Caso de Uso (`application/usecase/<Entidade>UseCase.java`):**
   - Anote com `@Service`; use `@Transactional(readOnly = true)` para consultas e `@Transactional` para escrita.
   - Injete **somente** interfaces `*Port` (`<Entidade>RepositoryPort`, `UserRepositoryPort`).
   - Resolva o usuário autenticado via `userRepository.findByEmail(email)` (`UserNotFoundException`) e passe `user.getId()` para todas as operações.
   - Consulte/altere/exclua por ID exclusivamente via `findByIdAndUserId(id, userId)` lançando `<Entidade>NotFoundException` se não encontrado (retorna `404` e evita IDOR).

## Passo 4: Camada `infrastructure` (Rotas e Persistência JPA)

Em `api-financas/src/main/java/peres/lucas/apifinanceira/infrastructure/`:

1. **Rotas (`infrastructure/config/ApiPaths.java`):**
   - Adicione a constante da rota sob `V1` (ex.: `INCOMES = V1 + "/rendas"`, `RECURRING_EXPENSES = V1 + "/gastos-recorrentes"`, `DEBTS = V1 + "/dividas"`, `CREDIT_CARDS = V1 + "/cartoes"`).
2. **Entidade JPA (`infrastructure/persistence/entity/<Entidade>Entity.java`):**
   - `@Entity`, `@Table(name = "tb_...")`, `@Getter`, `@Setter`.
   - Associe `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false) private UserEntity user;`.
3. **Repositório JPA (`infrastructure/persistence/repository/<Entidade>JpaRepository.java`):**
   - Estende `JpaRepository<*Entity, UUID>` com `findByIdAndUser_Id(UUID id, UUID userId)` e listagem por `user.id`.
4. **Adaptador (`infrastructure/persistence/adapter/<Entidade>PersistenceAdapter.java`):**
   - `@Component` implementando `*RepositoryPort`.
   - Associa `entity.setUser(userRepository.getReferenceById(domain.getUsuarioId()))`, persiste com `saveAndFlush` e converte entre `*Entity` e `domain.model.*`.

## Passo 5: Camada `adapter.web` (Controller e Exceções)

1. **Controller (`adapter/web/controller/<Entidade>Controller.java`):**
   - `@RestController`, `@RequestMapping(ApiPaths.<RECURSO>)`, `@Tag`, `@SecurityRequirement(name = "cookieAuth")`.
   - Extraia a identidade apenas de `Principal principal` (`principal.getName()`).
   - Use `@Valid @RequestBody` nas mutações e retorne `201 Created` no `POST`, `200 OK` no `GET`/`PUT`/`PATCH` e `204 No Content` no `DELETE`.
2. **Tratamento de Erro (`adapter/web/exception/GlobalExceptionHandler.java`):**
   - Adicione `@ExceptionHandler(<Entidade>NotFoundException.class)` retornando `HttpStatus.NOT_FOUND` com `ErrorMessage(HttpStatus.NOT_FOUND.value(), "Not Found", request.getRequestURI(), e.getMessage())`.

## Passo 6: Governança Arquitetural e Testes Backend

1. **Atualize a Fitness Function (`src/test/java/peres/lucas/apifinanceira/CleanArchitectureTest.java`):**
   - Adicione os novos modelos/enums/ports na lista `domainClasses`.
   - Adicione os novos casos de uso na lista `applicationUseCases`.
2. **Crie Testes Unitários de Domínio (`*Test.java`):**
   - Valide `trim()`, invariantes numéricas/faixas de dias, transições de status e exceções lançadas para entradas inválidas.
3. **Crie Teste de Integração `MockMvc` (`*FlowIntegrationTest.java`):**
   - Siga o padrão de `MovementFlowIntegrationTest`:
     - `401 Unauthorized` sem cookie `atona_access`.
     - `403 Forbidden` em `POST`/`PUT`/`PATCH`/`DELETE` sem cabeçalho `X-XSRF-TOKEN`.
     - `400 Bad Request` com `$.campos.*` para validações inválidas.
     - Isolamento entre usuários (`ana` e `bia`): tentativa de `GET`/`PUT`/`PATCH`/`DELETE` em recurso de outro usuário retorna `404 Not Found`.
     - Fluxo completo de criação, listagem, atualização e exclusão.
4. **Execute o Quality Gate Backend:**
   - Na pasta `api-financas`, execute `./mvnw verify` (ou `.\mvnw.cmd verify` no Windows) e garanta 100% dos testes passando e cobertura JaCoCo >= 80%.

## Passo 7: Frontend Angular 22 (`web-financas`)

1. **Core (`web-financas/src/app/core/<modulo>/`):**
   - Crie `*.models.ts` alinhado exatamente aos DTOs JSON do backend.
   - Crie `*.service.ts` (`@Injectable({ providedIn: 'root' })`, `inject(HttpClient)`, `inject(AuthService)`), encadeando `this.authService.ensureCsrf().pipe(switchMap(...))` em todo `POST`, `PUT`, `PATCH` e `DELETE`.
2. **Feature (`web-financas/src/app/features/<feature>/`):**
   - Crie Standalone Component com `PrivateHeaderComponent`, `ModalComponent`, `NonNullableFormBuilder`, `signal(...)`, `takeUntilDestroyed(this.destroyRef)` e tratamento de erro `401` / `ApiError`.
   - Use formatação monetária BRL (`pt-BR`) e cálculo/validação em centavos inteiros.
3. **Roteamento e Navegação:**
   - Registre a rota com `canActivate: [authGuard]` e `loadComponent` em `web-financas/src/app/app.routes.ts`.
   - Verifique o link correspondente no `PrivateHeaderComponent` e atualize o resumo em `/inicio` quando aplicável ao checklist da sprint.
4. **Testes & Build Frontend:**
   - Crie `*.service.spec.ts` e `*.component.spec.ts`.
   - Execute `npm test -- --watch=false` e `npm run build` dentro de `web-financas`.

## Passo 8: Atualização Documental

1. Marque os itens concluídos no checklist de `docs/plano-onda-2.md`.
2. Quando concluir a sprint ou consolidar a Onda 2, atualize a tabela de endpoints em `README.md`.
