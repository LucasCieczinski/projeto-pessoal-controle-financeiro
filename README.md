# À Tona — Controle Financeiro Pessoal

![Status do Projeto](https://img.shields.io/badge/status-em_desenvolvimento-blue?style=for-the-badge)
![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-21-DD0031?style=for-the-badge&logo=angular&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Rate_Limit-DC382D?style=for-the-badge&logo=redis&logoColor=white)

> **Propósito do Repositório:** Este é um **projeto pessoal de portfólio e laboratório de engenharia de software (sandbox de estudos e testes)**. O objetivo principal é aplicar na prática padrões avançados de arquitetura de software, segurança em aplicações web modernas, performance, testes automatizados e boas práticas de desenvolvimento Full Stack (Java + Angular) aplicados a um problema real: **clareza e controle financeiro para a vida como ela é**.

---

## 🎯 Sobre o Produto (`À Tona`)

A maioria das pessoas não perde o controle financeiro por falta de planilhas complexas, mas por falta de visibilidade rápida sobre o quanto entrou, o quanto saiu e o quanto realmente sobra no mês. O **À Tona** foi desenhado como uma aplicação web responsiva (desktop e mobile) focada em simplicidade visual, segurança de sessão e consistência contábil.

---

## 🏗️ Arquitetura & Engenharia de Software (Foco de Estudos)

O backend (`api-financas`) foi estruturado aplicando **Clean Architecture** combinada com **Arquitetura Hexagonal (Ports & Adapters)**, garantindo que o núcleo de regras de negócio seja 100% independente de frameworks, banco de dados ou detalhes de infraestrutura.

### 1. Organização em 4 Camadas Canônicas
```text
peres.lucas.apifinanceira
├── domain/                  # Núcleo puro (Zero dependências de Spring, JPA ou Jackson)
│   ├── model/               # Entidades de domínio ricas e imutáveis (User, Movement, AuthSession, MovementTotals)
│   ├── port/                # Contratos/Portas de saída (UserRepositoryPort, MovementRepositoryPort, RateLimitPort...)
│   └── exception/           # Exceções de regra de negócio
├── application/             # Orquestração dos fluxos da aplicação
│   ├── usecase/             # Casos de uso (*UseCase) transacionais
│   └── dto/                 # Contratos de entrada/saída da camada de aplicação
├── adapter/                 # Adaptadores de Entrada (Driving Adapters)
│   └── web/
│       ├── controller/      # Endpoints RESTful versionados (/api/v1/*)
│       └── exception/       # GlobalExceptionHandler & contrato padronizado de erros (ErrorMessage)
└── infrastructure/          # Adaptadores de Saída e Configurações Técnicas (Driven Adapters)
    ├── config/              # CacheConfig (Caffeine), OpenApiConfig, ApiPaths
    ├── persistence/         # Implementações JPA/Hibernate (*PersistenceAdapter, *Entity, *JpaRepository)
    └── security/            # Filtros JWT, Cookies HttpOnly, CSRF, Security Headers, InMemory & Redis Rate Limit
```

> **Fitness Function Arquitetural:** O projeto conta com o teste automatizado `CleanArchitectureTest`, que inspeciona o bytecode/classes a cada build para impedir que a camada `domain` importe anotações ou pacotes de frameworks externos (`org.springframework.*`, `jakarta.persistence.*`, etc.) ou que `UseCases` acessem repositórios JPA diretamente.

---

### 2. Segurança em Profundidade (Defense in Depth)
Em vez do padrão vulnerável de armazenar tokens JWT no `localStorage` do navegador (suscetível a ataques XSS), este projeto implementa uma arquitetura de sessão moderna:
- **Cookies `HttpOnly` + `SameSite=Lax`:** Access Token JWT de curta duração e Refresh Token trafegam exclusivamente em cookies inacessíveis via JavaScript.
- **Refresh Token Rotation & Revogação:** A cada renovação de sessão (`/api/v1/auth/refresh`), o token anterior é invalidado no banco (`AuthSession`) e substituído por um novo hash SHA-256.
- **Proteção CSRF (`CookieCsrfTokenRepository`):** Exigência do cabeçalho `X-XSRF-TOKEN` em todas as operações mutáveis (`POST`, `PUT`, `DELETE`).
- **Rate Limiting Distribuído e Local (Ports & Adapters):**
  - Implementação padrão em memória (`InMemoryRateLimitAdapter`) para execução local sem dependências externas.
  - Implementação distribuída com **Redis + Script Lua atômico** (`RedisRateLimitAdapter` com `INCR` + `PEXPIRE`), ativada via propriedade `app.rate-limit.storage=redis`.
- **HTTP Security Headers & Hardening:** Configurados `Content-Security-Policy (CSP)`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `HSTS` e bloqueio condicional do Swagger em produção (`app.swagger.enabled=false`).

---

### 3. Performance, Cache & Banco de Dados
- **Cache L1 em Memória com Caffeine (`CacheConfig`):** Redução de round-trips ao banco de dados para resolução de usuários autenticados (`@Cacheable` com prevenção de *negative caching* e invalidação automática via `@CacheEvict` ao salvar/atualizar).
- **Migrações Versionadas com Flyway:** Histórico de schema controlado em `src/main/resources/db/migration` (`V1` a `V3`), com índices otimizados para consultas financeiras por usuário e competência.
- **Cálculos Financeiros Precisos:** Uso estrito de `BigDecimal` e apuração consolidada de entradas, saídas, saldo do mês e saldo acumulado até o período selecionado.

---

### 4. Observabilidade, Qualidade, Testes & CI/CD
- **Observabilidade (`Spring Boot Actuator` + `Micrometer` + `MDC`):**
  - `CorrelationIdFilter` injetando e propagando `X-Correlation-Id` em todas as requisições HTTP e logs (`[corrId=...]`).
  - Endpoints `/actuator/health` e `/actuator/info` públicos para *health probes*, e `/actuator/metrics` protegido por autenticação (incluindo métricas de *hit/miss* do cache Caffeine).
- **Backend (`JUnit 5` + `MockMvc` + `Mockito` + `H2`):** Testes unitários de domínio, testes de adaptadores (incluindo simulação do Redis via Mockito), validação de arquitetura (`CleanArchitectureTest`) e testes de integração de ponta a ponta dos fluxos de autenticação, CSRF, observabilidade, contrato de erros e movimentações.
- **Frontend (`Angular 21` + `Vitest`):** Testes unitários e de integração de componentes, guardas de rota, interceptors de sessão/erro e serviços.
- **Integração Contínua (`GitHub Actions`):** Pipeline em `.github/workflows/ci.yml` executando builds e suítes de teste de Backend (Java 21) e Frontend (Node 22) em paralelo a cada push/PR.

---

### 5. Governança Arquitetural — ADRs (Architecture Decision Records)
Todas as decisões estruturais de engenharia, segurança e performance do projeto estão documentadas no diretório [`docs/adr/`](./docs/adr/):

| ADR | Título / Decisão | Status |
| :--- | :--- | :--- |
| [**ADR-000**](./docs/adr/ADR-000-template-padrao.md) | Template Padrão para Registro de Decisões Arquiteturais do Sistema | Modelo |
| [**ADR-001**](./docs/adr/ADR-001-clean-architecture-e-ports-and-adapters.md) | Adoção de Clean Architecture e Hexagonal (Ports & Adapters) com Fitness Function | Aceito |
| [**ADR-002**](./docs/adr/ADR-002-seguranca-sessao-cookies-httponly-e-csrf.md) | Arquitetura de Sessão Web Segura com Cookies `HttpOnly`, Refresh Token Rotation e CSRF | Aceito |
| [**ADR-003**](./docs/adr/ADR-003-rate-limiting-hibrido-redis-lua-e-cache-caffeine.md) | Rate Limiting Híbrido (In-Memory vs. Redis Lua Script) e Cache L1 com Caffeine | Aceito |
| [**ADR-004**](./docs/adr/ADR-004-monolito-modular-vs-microsservicos.md) | Estratégia de Monólito Modular vs. Microsserviços e Observabilidade | Aceito |

---

## 🔌 Principais Endpoints da API (`/api/v1`)

| Método | Rota | Descrição | Autenticação / CSRF |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/auth/csrf` | Emite o cookie `XSRF-TOKEN` para proteção CSRF | Público |
| `POST` | `/api/v1/users` | Cadastro de novo usuário | Público + CSRF + Rate Limit |
| `POST` | `/api/v1/auth/login` | Autenticação com emissão de cookies `HttpOnly` | Público + CSRF + Rate Limit |
| `POST` | `/api/v1/auth/refresh` | Rotação de Refresh Token e renovação do Access Token | Cookie Refresh + CSRF + Rate Limit |
| `POST` | `/api/v1/auth/logout` | Revogação da sessão no banco e limpeza dos cookies | Autenticado + CSRF |
| `GET` | `/api/v1/users/me` | Retorna os dados do perfil do usuário autenticado | Autenticado (Cookie JWT) |
| `GET` | `/api/v1/movimentacoes?ano=YYYY&mes=MM` | Lista movimentações da competência informada | Autenticado (Cookie JWT) |
| `GET` | `/api/v1/movimentacoes/resumo?ano=YYYY&mes=MM` | Retorna entradas, saídas, saldo do mês e acumulado | Autenticado (Cookie JWT) |
| `POST` | `/api/v1/movimentacoes` | Registra nova entrada ou saída financeira | Autenticado + CSRF |
| `PUT` | `/api/v1/movimentacoes/{id}` | Atualiza movimentação pertencente ao usuário | Autenticado + CSRF |
| `DELETE` | `/api/v1/movimentacoes/{id}` | Remove movimentação pertencente ao usuário | Autenticado + CSRF |
| `GET` | `/actuator/health` | Health check da aplicação (status UP/DOWN) | Público |
| `GET` | `/actuator/metrics` | Métricas da JVM, HikariCP e Cache Caffeine (Micrometer) | Autenticado (Cookie JWT) |

---

## 🚀 Como Executar o Projeto Localmente

### Pré-requisitos
- **Java JDK 21**
- **Node.js 20+ / 22 (LTS)**
- **PostgreSQL 16+** *(para rodar a aplicação local; a suíte de testes roda 100% em memória com H2 sem precisar de banco externo ou Docker)*

### 1. Executando o Backend (`api-financas`)
```bash
cd api-financas

# Configure as variáveis de ambiente (consulte o arquivo .env.example na raiz)
# Para rodar os testes automatizados (não requer PostgreSQL nem Redis instalados):
./mvnw test

# Para iniciar a API localmente (porta 8080):
./mvnw spring-boot:run
```

> **Nota sobre Rate Limit e Cache local:** Por padrão, a aplicação sobe com `app.rate-limit.storage=memory` e `app.cache.type=caffeine`, não exigindo servidor Redis local para desenvolvimento.

### 2. Executando o Frontend (`web-financas`)
```bash
cd web-financas

# Instalar dependências
npm install

# Rodar a suíte de testes unitários (Vitest)
npm test -- --watch=false

# Iniciar servidor de desenvolvimento (http://localhost:4200)
npm start
```

---

## 🗺️ Roadmap de Evolução (Ondas de Produto & Engenharia)

- [x] **Onda 1 — Fundação & Hardening:** Autenticação com cookies `HttpOnly` + Refresh rotativo + CSRF, CRUD completo de movimentações por competência, resumo financeiro mensal/acumulado e tratamento global de erros.
- [x] **Engenharia & Arquitetura (Pré-Onda 2):** Versionamento `/api/v1`, CI/CD no GitHub Actions, refatoração completa para Clean Architecture / Hexagonal com Fitness Function, Rate Limit (In-Memory / Redis Lua), Cache L1 com Caffeine, Observabilidade (Actuator, Micrometer, Correlation ID no MDC) e documentação de decisões arquiteturais (ADRs).
- [ ] **Melhorias Desejáveis de DevSecOps:** Cobertura de testes automatizada com JaCoCo no CI, verificação de pacote `.jar` e auditoria automática de dependências (Dependabot).
- [ ] **Onda 2 — Planejamento Financeiro:** Gestão de fontes de renda, cartões de crédito, dívidas/parcelamentos e gastos fixos/recorrentes.
- [ ] **Estudos Futuros:** Exploração de integração via Open Finance e arquitetura orientada a eventos.
