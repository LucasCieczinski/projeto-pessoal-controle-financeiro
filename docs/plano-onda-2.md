# 🌊 Plano de Implementação — Onda 2: Planejamento e Compromissos Financeiros (`À Tona`)

> **Princípio de Domínio da Onda 2 (Regra de Ouro da Migration V2):**
> *"A movimentação (`Movement`) representa dinheiro efetivamente realizado (caixa). Rendas, gastos recorrentes, cartões e dívidas são modelos de **planejamento e compromissos financeiros**, permitindo ao usuário enxergar seu custo de vida fixo e passivos antes mesmo do mês começar."*

---

## 📊 1. Visão Geral da Arquitetura da Onda 2

Todas as funcionalidades da Onda 2 seguem estritamente a estrutura das **4 Camadas Canônicas da Clean Architecture & Hexagonal** ([ADR-001](./adr/ADR-001-clean-architecture-e-ports-and-adapters.md)), protegidas pelo `CleanArchitectureTest` e pelo Quality Gate do **JaCoCo (>= 80%)** configurado no CI:

1. **`domain` (Java Puro):** Modelos ricos (`Income`, `RecurringExpense`, `Debt`, `DebtStatus`, `CreditCard`), exceções de negócio e portas (`*RepositoryPort`).
2. **`application`:** Casos de uso transacionais (`*UseCase`) e contratos DTO (`*RequestDTO`, `*ResponseDTO`).
3. **`adapter`:** Controladores RESTful versionados (`/api/v1/*`) e mapeamento de exceções no `GlobalExceptionHandler`.
4. **`infrastructure`:** Adaptadores JPA (`*PersistenceAdapter`, `*JpaRepository`, `*Entity`) e constantes de rota em `ApiPaths`.

---

## 🗄️ 2. Mapeamento do Banco de Dados (Migrations `V1` e `V2`)

As 4 tabelas da Onda 2 já estão criadas e validadas nas migrations `V1__create_financial_schema.sql` e `V2__align_financial_domain_rules.sql`:

| Tabela SQL | Modelo de Domínio (`domain.model`) | Colunas e Constraints Principais |
| :--- | :--- | :--- |
| `tb_rendas` | `Income` | `id` (UUID), `usuario_id` (FK), `fonte` (VARCHAR 100), `valor_base` (NUMERIC 12,2 `> 0`), `ativo` (BOOLEAN default `true`) |
| `tb_gastos_recorrentes` | `RecurringExpense` | `id` (UUID), `usuario_id` (FK), `descricao` (VARCHAR 150), `valor` (NUMERIC 12,2 `> 0`), `dia_vencimento` (INT `1..31`), `ativo` (BOOLEAN default `true`) |
| `tb_dividas` | `Debt` + `DebtStatus` | `id` (UUID), `usuario_id` (FK), `credor` (VARCHAR 100), `valor_total` (NUMERIC 12,2 `> 0`), `total_parcelas` (INT nullable `> 0`), `parcelas_pagas` (INT `>= 0` e `<= total_parcelas`), `status` (`ABERTA`, `QUITADA`, `ATRASADA`) |
| `tb_cartoes_credito` | `CreditCard` | `id` (UUID), `usuario_id` (FK), `nome_cartao` (VARCHAR 50), `limite` (NUMERIC 12,2 `>= 0`), `dia_fechamento` (INT `1..31`), `dia_vencimento` (INT `1..31`) |

---

## 🚀 3. Cronograma de Sprints (Fatias Verticais Full-Stack)

### Sprint 2.1 — Planejamento Mensal: Fontes de Renda (`Income`) & Gastos Recorrentes (`RecurringExpense`)
**Objetivo:** Permitir ao usuário mapear suas entradas fixas previstas e seus custos fixos mensais, calculando o **Comprometimento Fixo da Renda**.

#### Checklist Backend (`api-financas`)
- [x] **Constantes de Rota (`ApiPaths.java`):** Adicionar `INCOMES = API_V1 + "/rendas"` e `RECURRING_EXPENSES = API_V1 + "/gastos-recorrentes"`.
- [x] **Camada `domain`:**
  - [x] Criar `domain.model.Income` (higienização `trim()` de `fonte`, validação `valorBase > 0`, flag `ativo`).
  - [x] Criar `domain.model.RecurringExpense` (higienização `trim()` de `descricao`, validação `valor > 0`, `diaVencimento` entre `1` e `31`, flag `ativo`).
  - [x] Criar `domain.exception.IncomeNotFoundException` e `RecurringExpenseNotFoundException`.
  - [x] Criar `domain.port.IncomeRepositoryPort` e `domain.port.RecurringExpenseRepositoryPort`.
- [x] **Camada `application`:**
  - [x] Criar DTOs (`IncomeRequestDto`, `IncomeResponseDto`, `RecurringExpenseRequestDto`, `RecurringExpenseResponseDto`).
  - [x] Criar `application.usecase.IncomeUseCase` e `application.usecase.RecurringExpenseUseCase`.
- [x] **Camada `infrastructure`:**
  - [x] Criar `IncomeEntity`, `IncomeJpaRepository` e `IncomePersistenceAdapter`.
  - [x] Criar `RecurringExpenseEntity`, `RecurringExpenseJpaRepository` e `RecurringExpensePersistenceAdapter`.
- [x] **Camada `adapter` (Web):**
  - [x] Criar `IncomeController` (`/api/v1/rendas`) e `RecurringExpenseController` (`/api/v1/gastos-recorrentes`).
- [x] **Testes Automatizados:**
  - [x] Testes unitários de domínio (`IncomeTest`, `RecurringExpenseTest`).
  - [x] Teste de integração `MockMvc` (`PlanningFlowIntegrationTest`).

#### Checklist Frontend (`web-financas`)
- [x] Criar `PlanningService` consumindo `/api/v1/rendas` e `/api/v1/gastos-recorrentes`.
- [x] Criar tela/componente de **Planejamento Fixo** (Fontes de Renda e Gastos Recorrentes).
- [x] Exibir indicador de **Renda Prevista vs. Gastos Fixos** no resumo (`/inicio`).
- [x] Testes unitários no Vitest.

---

### Sprint 2.2 — Controle de Passivos: Dívidas e Parcelamentos (`Debt`)
**Objetivo:** Visibilidade total às dívidas e parcelamentos do usuário, com cálculo automático de progresso de quitação e regras ricas de transição de status no domínio.

#### Checklist Backend (`api-financas`)
- [ ] **Constantes de Rota (`ApiPaths.java`):** Adicionar `DEBTS = API_V1 + "/dividas"`.
- [ ] **Camada `domain`:**
  - [ ] Criar enum `domain.model.DebtStatus` (`ABERTA`, `QUITADA`, `ATRASADA`).
  - [ ] Criar `domain.model.Debt` com invariantes (`valorTotal > 0`, `parcelasPagas <= totalParcelas`, transição automática para `QUITADA` ao quitar todas as parcelas e método `registrarPagamentoParcela()`).
  - [ ] Criar `domain.exception.DebtNotFoundException` e `domain.port.DebtRepositoryPort`.
- [ ] **Camada `application`, `infrastructure` & `adapter`:**
  - [ ] Criar `DebtRequestDTO`, `DebtResponseDTO` e `DebtUseCase`.
  - [ ] Criar `DebtEntity`, `DebtJpaRepository` e `DebtPersistenceAdapter`.
  - [ ] Criar `DebtController` (`/api/v1/dividas` + `PATCH /api/v1/dividas/{id}/pagar-parcela`).
- [ ] **Testes Automatizados:**
  - [ ] `DebtTest` + `DebtFlowIntegrationTest`.

#### Checklist Frontend (`web-financas`)
- [ ] Criar interface de **Dívidas & Parcelamentos** com barra de progresso (`parcelasPagas / totalParcelas`), filtro por status e ação rápida de pagar parcela.
- [ ] Testes unitários no Vitest.

---

### Sprint 2.3 — Cartões de Crédito (`CreditCard`) & Consolidação da Onda 2
**Objetivo:** Gerenciamento de cartões de crédito (limites, datas de fechamento e vencimento) e consolidação do panorama financeiro da Onda 2.

#### Checklist Backend (`api-financas`)
- [ ] **Constantes de Rota (`ApiPaths.java`):** Adicionar `CREDIT_CARDS = API_V1 + "/cartoes"`.
- [ ] **Camada `domain`:**
  - [ ] Criar `domain.model.CreditCard` (validações: `limite >= 0`, `diaFechamento` entre `1..31`, `diaVencimento` entre `1..31`).
  - [ ] Criar `domain.exception.CreditCardNotFoundException` e `domain.port.CreditCardRepositoryPort`.
- [ ] **Camada `application`, `infrastructure` & `adapter`:**
  - [ ] Criar `CreditCardRequestDTO`, `CreditCardResponseDTO` e `CreditCardUseCase`.
  - [ ] Criar `CreditCardEntity`, `CreditCardJpaRepository` e `CreditCardPersistenceAdapter`.
  - [ ] Criar `CreditCardController` (`/api/v1/cartoes`).
- [ ] **Testes Automatizados & Documentação:**
  - [ ] `CreditCardTest` + `CreditCardFlowIntegrationTest`.
  - [ ] Atualizar `README.md` com as novas rotas da Onda 2.

#### Checklist Frontend (`web-financas`)
- [ ] Criar seção/tela de **Cartões de Crédito** exibindo limite total, dia de fechamento e dia de vencimento.
- [ ] Testes unitários no Vitest.

---

## ✅ 4. Definição de Pronto (Definition of Done — DoD)
1. **Clean Architecture Íntegra:** `CleanArchitectureTest` passando 100%.
2. **Isolamento de Dados:** Todo recurso pertence estritamente ao `usuarioId` autenticado via cookie `HttpOnly` (`atona_access`); tentativas de acessar ou alterar IDs de outro usuário retornam `404 Not Found`.
3. **Quality Gate no CI:** Cobertura de linhas medida pelo JaCoCo em `./mvnw verify` >= 80% e 100% dos testes do Vitest no frontend passando.
