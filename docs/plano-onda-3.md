# 📈 Plano de Implementação — Onda 3: Visão Analítica, Cofrinho de Metas e Lista de Desejos (`À Tona`)

> **Princípio de Domínio da Onda 3 (Continuidade da Regra de Ouro da Migration V2):**
> - **Histórico 12 Meses (`Movement`):** consulta analítica somente leitura sobre o livro-caixa realizado (`tb_movimentacoes`), sem alterar registros ou regras de cálculo existentes.
> - **Cofrinho de Metas (`Goal` / `tb_metas`) e Lista de Desejos (`WishlistItem` / `tb_lista_desejos`):** entidades de objetivos e priorização financeira; mantêm a regra de **não alterar automaticamente o saldo realizado** de `tb_movimentacoes` e estruturam a base de dados que alimentará a **Onda 4 (Motor de Recomendação de Compra)**.

---

## 🏗️ 1. Arquitetura, Governança Multi-Agente e Guardrails

Todas as entregas da Onda 3 seguem integralmente:
- **4 Camadas Canônicas da Clean Architecture & Hexagonal** ([ADR-001](./adr/ADR-001-clean-architecture-e-ports-and-adapters.md)): `domain` (100% Java puro), `application`, `adapter` e `infrastructure`, protegidas por [`CleanArchitectureTest.java`](../api-financas/src/test/java/peres/lucas/apifinanceira/CleanArchitectureTest.java).
- **Regra de Banco de Dados (Migrations Flyway):** nunca editar `V1`, `V1_1`, `V2` ou `V3`. Novas tabelas devem nascer em migrações incrementais compatíveis com PostgreSQL e alinhadas às entidades JPA validadas no H2 (`application-test.yaml`):
  - **Sprint 3.1:** sem nova migration (agregação direta em `tb_movimentacoes`).
  - **Sprint 3.2:** `V4__create_goals.sql` (`tb_metas`).
  - **Sprint 3.3:** `V5__create_wishlist.sql` (`tb_lista_desejos`).
- **Orquestração da Squad por Sprint:**
  1. `java-hexagonal-architect` (Backend) + `angular-ui-builder` (Frontend) em paralelo.
  2. `fintech-sec-qa-reviewer` executando o checklist `fintech-security-audit` após ambos concluírem.
  3. Atualização deste checklist e do `README.md`.
- **Guardrails Técnicos Incorporados da Onda 2:**
  - Precisão monetária estrita: `BigDecimal` (`NUMERIC(12, 2)`) no backend Java e aritmética em centavos inteiros no frontend TypeScript.
  - Quando o domínio permitir valor zero (como `valorAtual >= 0` em metas), usar `parseNonNegativeAmount` (nunca reutilizar `parseAmount`, que rejeita `0`).
  - Em todos os `<input type="number">` com Angular Reactive Forms (`NumberValueAccessor`), tipar o `FormControl` como `string | number | null`, normalizar sempre com `String(... ?? '').trim()` nos validadores e no `save()`, e incluir teste de regressão DOM disparando `new Event('input')`.
  - Navegação mobile limpa: incluir destinos secundários em `#mobile-account-menu` (menu `Mais`), evitando comprimir a `.bottom-nav`.

---

## 🗄️ 2. Modelagem de Dados (Migrations `V4` e `V5`)

### Migration `V4__create_goals.sql` (`tb_metas`)
```sql
CREATE TABLE tb_metas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES tb_usuarios (id) ON DELETE CASCADE,
    nome VARCHAR(100) NOT NULL,
    valor_alvo NUMERIC(12, 2) NOT NULL,
    valor_atual NUMERIC(12, 2) NOT NULL DEFAULT 0,
    prazo DATE,
    status VARCHAR(20) NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_metas_valor_alvo_positivo CHECK (valor_alvo > 0),
    CONSTRAINT chk_metas_valor_atual_nao_negativo CHECK (valor_atual >= 0),
    CONSTRAINT chk_metas_status CHECK (status IN ('EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA'))
);

CREATE INDEX idx_metas_usuario_status
    ON tb_metas (usuario_id, status);
```

### Migration `V5__create_wishlist.sql` (`tb_lista_desejos`)
```sql
CREATE TABLE tb_lista_desejos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES tb_usuarios (id) ON DELETE CASCADE,
    nome VARCHAR(120) NOT NULL,
    valor_estimado NUMERIC(12, 2) NOT NULL,
    prioridade VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    observacao VARCHAR(255),
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_desejos_valor_positivo CHECK (valor_estimado > 0),
    CONSTRAINT chk_desejos_prioridade CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA')),
    CONSTRAINT chk_desejos_status CHECK (status IN ('DESEJADO', 'COMPRADO', 'CANCELADO'))
);

CREATE INDEX idx_lista_desejos_usuario_status
    ON tb_lista_desejos (usuario_id, status);
```

---

## 🚀 3. Cronograma de Sprints (Fatias Verticais Full-Stack)

### Sprint 3.1 — Visão Analítica: Comparação Mensal dos Últimos 12 Meses (`GET /api/v1/movimentacoes/historico`)
**Objetivo:** Permitir que o usuário enxergue a evolução mensal de **Entradas × Saídas × Resultado** ao longo dos últimos meses (padrão 12 meses) diretamente no painel `/inicio`, sem adicionar migrations ou dependências pesadas de gráficos.

#### Checklist Backend (`api-financas`)
- [ ] **Camada `domain` (Java puro):**
  - [ ] Criar `domain.model.MonthlyMovementTotal` (ou record de domínio puro) representando ano, mês, total de entradas (`BigDecimal`), total de saídas (`BigDecimal`) e saldo/resultado calculado em `BigDecimal`.
  - [ ] Estender `domain.port.MovementRepositoryPort` com `List<MonthlyMovementTotal> summarizeByMonth(UUID userId, LocalDate start, LocalDate end)`.
- [ ] **Camada `infrastructure`:**
  - [ ] Adicionar em `MovementJpaRepository` projeção/query JPQL única agregando por `year(m.dataMovimentacao)` e `month(m.dataMovimentacao)` filtrada por `m.user.id = :userId` e faixa de datas `[start, end)` (sem queries N+1).
  - [ ] Implementar `summarizeByMonth` no `MovementPersistenceAdapter` (com suporte a dados esparsos).
- [ ] **Camada `application` & `adapter`:**
  - [ ] Criar `application.dto.MonthlyHistoryItemDto` (`ano`, `mes`, `entradas`, `saidas`, `resultado`) e `application.dto.MovementHistoryResponseDto` (`referencia`, `meses`, `itens`).
  - [ ] Implementar `MovementUseCase.historicoMensal(YearMonth referencia, int meses, String email)` (`@Transactional(readOnly = true)`), validando `meses` entre `1` e `24` (default `12`, lançando `InvalidRequestException` fora do intervalo) e preenchendo meses sem lançamentos com `0.00` em ordem cronológica crescente.
  - [ ] Expor `GET /api/v1/movimentacoes/historico` em `MovementController` aceitando `?mes=YYYY-MM` (opcional, default mês atual) e `?meses=12` (opcional, default `12`), extraindo identidade exclusivamente de `Principal`.
- [ ] **Testes & Governança:**
  - [ ] Atualizar `CleanArchitectureTest` caso novas classes/ports sejam adicionadas ao `domain`.
  - [ ] Adicionar testes unitários (`MovementUseCaseTest` / domínio) cobrindo preenchimento de meses vazios, ordenação cronológica e validação de `meses`.
  - [ ] Estender `MovementFlowIntegrationTest` (`MockMvc`) validando `401` sem sessão, `400` para parâmetros inválidos (`meses=0` ou `meses=25`), isolamento entre usuários (`ana` vs `bia`) e agregação precisa dos últimos 12 meses.

#### Checklist Frontend (`web-financas`)
- [ ] Estender `core/movements/movement.models.ts` (`MonthlyHistoryItem`, `MovementHistoryResponse`) e `MovementService.history(month: string, months = 12)`.
- [ ] Atualizar `movement.service.spec.ts` cobrindo a chamada `GET /api/v1/movimentacoes/historico`.
- [ ] Criar componente standalone reutilizável `MonthlyComparisonChartComponent` em `src/app/shared/monthly-comparison-chart/` usando SVG nativo leve, responsivo e acessível (comparando Entradas × Saídas × Resultado, toggle `6 meses` / `12 meses`, estado vazio e leitura via teclado/screen reader).
- [ ] Integrar o gráfico comparativo no `HomeComponent` (`/inicio`) acompanhando a competência selecionada no `PeriodNavigationComponent` (com fallback gracioso sem quebrar os cards principais).
- [ ] Criar `monthly-comparison-chart.component.spec.ts` e atualizar `home.component.spec.ts` no Vitest.

---

### Sprint 3.2 — Cofrinho de Metas Financeiras (`Goal` / `/api/v1/metas`)
**Objetivo:** Permitir ao usuário criar metas de economia/reserva (ex.: *Reserva de Emergência*, *Viagem*), registrar aportes e resgates com regras ricas de transição de status no domínio e acompanhar o ritmo mensal necessário até o prazo.

#### Checklist Backend (`api-financas`)
- [ ] **Banco & Rotas:**
  - [ ] Criar migration Flyway `V4__create_goals.sql` (`tb_metas`).
  - [ ] Adicionar `public static final String GOALS = V1 + "/metas";` em `ApiPaths.java`.
- [ ] **Camada `domain` (100% Java puro):**
  - [ ] Criar enum `domain.model.GoalStatus` (`EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`).
  - [ ] Criar `domain.model.Goal` com `id`, `usuarioId` e `dataCriacao` `final`; `nome` com `.trim()` (`1..100`), `valorAlvo > 0` (`BigDecimal`), `valorAtual >= 0` (`BigDecimal`), `prazo` (`LocalDate` opcional), `status`.
  - [ ] Implementar métodos ricos de comportamento:
    - `atualizarDados(...)`: atualiza metadados e promove para `CONCLUIDA` quando aplicável (se `status != CANCELADA` e `valorAtual >= valorAlvo`).
    - `aportar(BigDecimal valor)`: exige `valor > 0`, bloqueia se `status == CANCELADA`, soma ao `valorAtual` e transiciona automaticamente para `CONCLUIDA` quando `valorAtual.compareTo(valorAlvo) >= 0`.
    - `resgatar(BigDecimal valor)`: exige `valor > 0` e `valor <= valorAtual`, bloqueia se `status == CANCELADA`, subtrai de `valorAtual` e reabre para `EM_ANDAMENTO` se `valorAtual.compareTo(valorAlvo) < 0`.
  - [ ] Criar `domain.exception.GoalNotFoundException` e `domain.port.GoalRepositoryPort` (`save`, `findByUserId`, `findByIdAndUserId`, `delete`).
- [ ] **Camada `application`, `infrastructure` & `adapter`:**
  - [ ] Criar `GoalRequestDto`, `GoalAmountOperationRequestDto` (`valor > 0`) e `GoalResponseDto`.
  - [ ] Criar `GoalUseCase` (`listar`, `criar`, `atualizar`, `aportar`, `resgatar`, `excluir`), sempre resolvendo o usuário por `Principal` e consultando via `findByIdAndUserId`.
  - [ ] Criar `GoalEntity`, `GoalJpaRepository` e `GoalPersistenceAdapter`.
  - [ ] Criar `GoalController` (`GET`, `POST`, `PUT /{id}`, `PATCH /{id}/aportar`, `PATCH /{id}/resgatar`, `DELETE /{id}` em `/api/v1/metas`) e handler `404` em `GlobalExceptionHandler`.
- [ ] **Testes & Governança:**
  - [ ] Atualizar `CleanArchitectureTest` (`Goal`, `GoalStatus`, `GoalRepositoryPort`, `GoalUseCase`).
  - [ ] Criar `GoalTest` (testando aportes, resgates, promoções/reaberturas automáticas, bloqueio em `CANCELADA`, valores inválidos).
  - [ ] Criar `GoalFlowIntegrationTest` (`401`, `403` sem CSRF em `POST`/`PUT`/`PATCH`/`DELETE`, `400`, `404` anti-IDOR entre `ana` e `bia` em todas as rotas por ID, CRUD + aporte + resgate).

#### Checklist Frontend (`web-financas`)
- [ ] Criar `core/goals/` (`goal.models.ts`, `goal.service.ts` com `ensureCsrf()` em toda mutação, `goal.service.spec.ts`).
- [ ] Criar feature `/metas` (`features/goals/goals.component.ts`, `.html`, `.scss`, `.spec.ts`):
  - [ ] KPIs em centavos inteiros: total acumulado nos cofrinhos, total alvo ativo e metas concluídas.
  - [ ] Cards por meta com barra de progresso, aporte mensal sugerido quando houver `prazo`, filtro por status (`TODOS`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`), modal de aporte/resgate e CRUD completo.
  - [ ] Usar `parseNonNegativeAmount` no campo `valorAtual` (aceitando `0,00`) e `parseAmount` em `valorAlvo` e no valor da operação de aporte/resgate.
- [ ] Registrar rota `/metas` protegida por `authGuard` em `app.routes.ts` e adicionar navegação em `PrivateHeaderComponent` (desktop `.private-nav` e mobile `#mobile-account-menu`).
- [ ] Validar `npm test -- --watch=false` e `npm run build`.

---

### Sprint 3.3 — Lista de Desejos (`WishlistItem` / `/api/v1/desejos`) & Consolidação da Onda 3
**Objetivo:** Permitir ao usuário registrar desejos de compra com valor estimado e prioridade (`BAIXA`, `MEDIA`, `ALTA`), organizando desejos pendentes para futura avaliação do motor da Onda 4.

#### Checklist Backend (`api-financas`)
- [ ] **Banco & Rotas:**
  - [ ] Criar migration Flyway `V5__create_wishlist.sql` (`tb_lista_desejos`).
  - [ ] Adicionar `public static final String WISHLIST = V1 + "/desejos";` em `ApiPaths.java`.
- [ ] **Camada `domain` (100% Java puro):**
  - [ ] Criar enums `domain.model.WishlistPriority` (`BAIXA`, `MEDIA`, `ALTA`) e `domain.model.WishlistStatus` (`DESEJADO`, `COMPRADO`, `CANCELADO`).
  - [ ] Criar `domain.model.WishlistItem` com `id`, `usuarioId` e `dataCriacao` `final`; `nome` com `.trim()` (`1..120`), `valorEstimado > 0` (`BigDecimal`), `prioridade`, `status`, `observacao` opcional (`trim()` se preenchida, máx. `255`), e métodos ricos `marcarComoComprado()` e `cancelar()`.
  - [ ] Criar `domain.exception.WishlistItemNotFoundException` e `domain.port.WishlistRepositoryPort` (`save`, `findByUserId`, `findByIdAndUserId`, `delete`).
- [ ] **Camada `application`, `infrastructure` & `adapter`:**
  - [ ] Criar `WishlistItemRequestDto`, `WishlistItemResponseDto` e `WishlistUseCase`.
  - [ ] Criar `WishlistItemEntity`, `WishlistItemJpaRepository` e `WishlistPersistenceAdapter`.
  - [ ] Criar `WishlistController` (`GET`, `POST`, `PUT /{id}`, `PATCH /{id}/comprar`, `DELETE /{id}` em `/api/v1/desejos`) e handler `404` em `GlobalExceptionHandler`.
- [ ] **Testes & Governança:**
  - [ ] Atualizar `CleanArchitectureTest` (`WishlistItem`, `WishlistPriority`, `WishlistStatus`, `WishlistRepositoryPort`, `WishlistUseCase`).
  - [ ] Criar `WishlistItemTest` + `WishlistFlowIntegrationTest` (`401`, `403` sem CSRF, `400`, `404` anti-IDOR entre `ana` e `bia` em `PUT`/`PATCH`/`DELETE`, CRUD completo).

#### Checklist Frontend (`web-financas`)
- [ ] Criar `core/wishlist/` (`wishlist.models.ts`, `wishlist.service.ts` com `ensureCsrf()`, `wishlist.service.spec.ts`).
- [ ] Criar feature `/desejos` (`features/wishlist/wishlist.component.ts`, `.html`, `.scss`, `.spec.ts`):
  - [ ] KPIs em centavos inteiros: total estimado em desejos pendentes (`DESEJADO`), quantidade por prioridade alta e itens já conquistados (`COMPRADO`).
  - [ ] Filtros por status (`TODOS`, `DESEJADO`, `COMPRADO`, `CANCELADO`) e prioridade (`TODAS`, `ALTA`, `MEDIA`, `BAIXA`), ação rápida `"Marcar como comprado"`, modal de criação/edição e exclusão.
- [ ] Registrar rota `/desejos` protegida por `authGuard` em `app.routes.ts` e adicionar navegação em `PrivateHeaderComponent`.
- [ ] Atualizar `README.md` documentando as novas rotas da Onda 3 e marcando a Onda 3 como concluída no Roadmap.
- [ ] Validar `npm test -- --watch=false` e `npm run build`.

---

## ✅ 4. Definição de Pronto (Definition of Done — DoD) da Onda 3
1. **Clean Architecture Íntegra:** [`CleanArchitectureTest.java`](../api-financas/src/test/java/peres/lucas/apifinanceira/CleanArchitectureTest.java) passando 100% em todas as sprints.
2. **Isolamento Multi-Tenant e CSRF:** Identidade extraída exclusivamente de `Principal`, consultas por ID via `findByIdAndUserId` retornando `404 Not Found` para acessos cruzados, e proteção `ensureCsrf()` + `X-XSRF-TOKEN` em todas as mutações.
3. **Precisão Contábil e Separação de Escopo:** Uso exclusivo de `BigDecimal` no backend e centavos inteiros no frontend, preservando o saldo realizado de `tb_movimentacoes` intacto nas operações de metas e desejos.
4. **Quality Gates & Auditoria:** `mvnw.cmd verify` com cobertura JaCoCo $\ge 80\%$, `npm test -- --watch=false` e `npm run build` sem erros, e parecer **APROVADO** do subagente auditor `fintech-sec-qa-reviewer` (`fintech-security-audit`) ao final de cada sprint.
