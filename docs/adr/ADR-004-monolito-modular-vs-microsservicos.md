# ADR-004: Estratégia de Monólito Modular vs. Microsserviços e Observabilidade

- **Status:** Aceito
- **Data:** 2026-11-19
- **Autor(es):** Lucas Cieczinski Peres
- **Escopo:** Arquitetura de Sistema e Observabilidade

---

## 1. Contexto e Problema

Ao projetar um sistema moderno visando portfólio de Engenharia de Software e evolução para novas ondas de produto (Onda 2: Rendas, Cartões, Dívidas e Gastos Recorrentes; Onda 3: Integrações Open Finance), surge a questão arquitetural fundamental:
**Devemos fatiar o sistema imediatamente em múltiplos Microsserviços (ex.: `auth-service`, `movement-service`, `planning-service`) ou construir um Monólito Modular bem isolado?**

Além disso, independentemente da topologia de deploy, sistemas profissionais exigem rastreabilidade de logs e monitoramento de saúde em tempo de execução.

---

## 2. Opções Consideradas

1. **Arquitetura de Microsserviços Distribuídos desde o Dia 1:**
   - *Prós:* Isolamento físico de deploy e banco de dados por serviço.
   - *Contras:* Altíssimo custo operacional (exige múltiplos containers, Service Discovery, API Gateway, transações distribuídas/Saga Pattern, latência de rede entre chamadas internas e complexidade de execução local sem Docker).
2. **Monólito Modular com Clean Architecture / Ports & Adapters ("Modular Monolith First"):**
   - *Prós:* Transações ACID simples e rápidas no mesmo banco PostgreSQL; zero latência de rede entre autenticação, usuários e movimentações; deploy único e simples; fronteiras lógicas tão bem definidas (`Port` / `UseCase`) que qualquer módulo pode ser extraído no futuro caso haja necessidade real.
   - *Contras:* Compartilha o mesmo processo JVM e banco de dados físico inicialmente.

---

## 3. Decisão Tomada

### 3.1. Adoção do Padrão *Modular Monolith First*
Decidimos manter o backend como um **Monólito Modular** estruturado sob Clean Architecture e Hexagonal ([ADR-001](./ADR-001-clean-architecture-e-ports-and-adapters.md)).
- Os contextos de **Identidade & Sessão (`User`, `AuthSession`)** e **Movimentações Financeiras (`Movement`, `MovementTotals`)** comunicam-se apenas através de contratos claros (`*Port` e `*UseCase`), sem acoplamento direto entre repositórios JPA nos casos de uso.

### 3.2. Critérios Técnicos (Gatilhos) para Futura Extração de Microsserviços
Documentamos explicitamente que um módulo só deverá ser extraído para um microsserviço independente caso atinja um dos seguintes gatilhos reais:
1. **Assimetria Extrema de Carga/Escala:** Ex.: Um futuro módulo de *Sincronização Open Finance / Processamento de Webhooks Bancários* que receba picos intensos de I/O assíncrono e precise escalar independentemente da API principal.
2. **Isolamento Regulatório / Segurança de Credenciais Bancárias:** Necessidade de isolar tokens OAuth2 de instituições financeiras externas em uma rede/banco segregado.

### 3.3. Observabilidade Embutida desde a Base
Para garantir visibilidade digna de sistemas distribuídos mesmo dentro de um Monólito Modular, implementamos:
- **Rastreabilidade por Requisição (`CorrelationIdFilter` + SLF4J MDC):** Toda requisição HTTP recebe ou preserva o cabeçalho `X-Correlation-Id`, injetado no MDC (`[corrId=...]`) com precedência máxima (`Ordered.HIGHEST_PRECEDENCE`) e devolvido na resposta HTTP.
- **Monitoramento e Métricas (`Spring Boot Actuator` + `Micrometer`):**
  - `GET /actuator/health` e `/actuator/info` expostos publicamente (com detalhes internos restritos a `when_authorized`) para *health probes*.
  - `GET /actuator/metrics` protegido por autenticação, coletando métricas da JVM, pool de conexões HikariCP e estatísticas de *Hit/Miss* do cache Caffeine (`.recordStats()`).

---

## 4. Consequências e Trade-offs

### ✅ Pontos Positivos (Ganhos)
- **Pragmatismo de Engenharia Sênior:** Evita o anti-padrão de *Microsserviços Prematuros (Distributed Monolith)*, mantendo alta velocidade de entrega para a Onda 2 sem sacrificar a qualidade arquitetural.
- **Diagnóstico Rápido em Produção:** Qualquer erro reportado pelo frontend pode ser rastreado instantaneamente nos logs pelo `X-Correlation-Id`.

### ⚠️ Pontos de Atenção / Custos Aceitos (Trade-offs)
- Exige disciplina contínua para manter os casos de uso desacoplados dentro do mesmo repositório (garantida pelo `CleanArchitectureTest`).

---

## 5. Validação e Governança (Fitness Functions / Testes)

- [`ObservabilityIntegrationTest.java`](../../api-financas/src/test/java/peres/lucas/apifinanceira/ObservabilityIntegrationTest.java): Valida a disponibilidade pública do `/actuator/health`, a geração e propagação do `X-Correlation-Id` e o bloqueio de segurança (`401 Unauthorized`) no `/actuator/metrics`.
