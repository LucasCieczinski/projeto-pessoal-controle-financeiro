# Regras Globais do Produto — À Tona (`projeto-pessoal-controle-financeiro`)

Este arquivo define as diretrizes inegociáveis de domínio, segurança bancária, governança arquitetural e orquestração multi-agente do projeto **À Tona**.

## 1. Princípio de Domínio (Regra de Ouro da Migration V2)

- **Caixa Realizado (`Movement` / `tb_movimentacoes`):** representa exclusivamente dinheiro efetivamente realizado (entradas e saídas ocorridas).
- **Planejamento e Compromissos (`Onda 2`):**
  - `Income` (`tb_rendas`): fontes de renda previstas (`valor_base > 0`, `ativo`).
  - `RecurringExpense` (`tb_gastos_recorrentes`): compromissos mensais recorrentes (`valor > 0`, `dia_vencimento` `1..31`, `ativo`).
  - `Debt` (`tb_dividas`): passivos e parcelamentos (`valor_total > 0`, `total_parcelas > 0` ou `null`, `0 <= parcelas_pagas <= total_parcelas`, `status` em `ABERTA`, `QUITADA`, `ATRASADA`).
  - `CreditCard` (`tb_cartoes_credito`): cartões e limites (`limite >= 0`, `dia_fechamento` `1..31`, `dia_vencimento` `1..31`).
- **Regra contábil:** entidades de planejamento **não** alteram o saldo realizado (`tb_movimentacoes`) automaticamente sem que haja uma movimentação explícita originada.

## 2. Invariantes Inegociáveis de Engenharia Financeira

### 2.1. Precisão Monetária (`BigDecimal` e Centavos)
- **Backend Java:** valores monetários usam **exclusivamente** `java.math.BigDecimal` mapeado para `NUMERIC(12, 2)`. É proibido usar `double` ou `float` em modelos, DTOs, entidades ou cálculos financeiros.
- **Frontend TypeScript:** valores monetários são validados e somados em centavos inteiros (`Math.round(valor * 100)`, `Number.isSafeInteger`, padrão `parseAmount`) e exibidos com `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`.

### 2.2. Isolamento Multi-Tenant e Prevenção de IDOR
- Todo recurso financeiro pertence estritamente a um único `usuarioId`.
- A identidade do usuário autenticado **deve sempre** ser extraída de `java.security.Principal` (`principal.getName()`) preenchido pelo cookie `HttpOnly` (`atona_access`) no filtro JWT (`JwtAuthenticationFilter`), **nunca** de campos enviados no corpo JSON, query params ou argumentos decididos por modelos de IA (prevenção de *IDOR* e *Prompt Injection* em futuras Tools/MCPs).
- Consultas e mutações por ID devem usar métodos escopados pelo dono (`findByIdAndUserId(id, userId)`). Tentativas de acessar, editar ou excluir registros de outro usuário devem retornar **`404 Not Found`** (nunca `403` com vazamento de existência).

### 2.3. Segurança Web (`HttpOnly` + CSRF + Headers)
- Nunca armazene JWTs ou refresh tokens em `localStorage`/`sessionStorage`.
- Toda operação mutável (`POST`, `PUT`, `PATCH`, `DELETE`) protegida por sessão exige o cabeçalho `X-XSRF-TOKEN` no backend e o encadeamento de `authService.ensureCsrf()` no Angular.

### 2.4. Governança por ADRs
- Antes de introduzir novas dependências estruturais, integrações de IA/MCP ou padrões arquiteturais, consulte os ADRs em `docs/adr/` (`ADR-001` a `ADR-004`).
- Decisões estruturais novas devem ser registradas em `docs/adr/ADR-XXX-<slug>.md` seguindo o template oficial `docs/adr/ADR-000-template-padrao.md` (use a skill `adr-writer`).

## 3. Fluxo Multi-Agente e Skills do Projeto

- **Skills disponíveis em `.agents/skills/`:**
  - `onda2-vertical-slice`: runbook ponta a ponta (Domain -> Application -> Infrastructure -> Adapter -> Testes -> Angular) para implementar recursos da Onda 2 conforme `docs/plano-onda-2.md`.
  - `fintech-security-audit`: checklist de AppSec bancária (Anti-IDOR, CSRF, `BigDecimal`, `CleanArchitectureTest`, JaCoCo >= 80% e Vitest).
  - `adr-writer`: criação padronizada de ADRs e atualização do índice no `README.md`.
- **Subagentes especialistas em `.agents/agents/`:**
  - `java-hexagonal-architect`: desenvolvimento Backend Java 21 / Spring Boot 4.1 em `api-financas/`.
  - `angular-ui-builder`: construção de interfaces Angular 22 e testes Vitest em `web-financas/`.
  - `fintech-sec-qa-reviewer`: auditoria de segurança bancária, conformidade arquitetural e qualidade de testes após implementações.
