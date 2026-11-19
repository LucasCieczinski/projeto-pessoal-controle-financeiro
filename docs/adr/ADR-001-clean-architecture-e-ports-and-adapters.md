# ADR-001: Adoção de Clean Architecture e Hexagonal (Ports & Adapters) com Fitness Function

- **Status:** Aceito
- **Data:** 2026-11-15
- **Autor(es):** Lucas Cieczinski Peres
- **Escopo:** Backend (`api-financas`)

---

## 1. Contexto e Problema

Em sistemas financeiros, as regras de negócio (cálculo de saldo do mês, saldo acumulado por competência, isolamento de lançamentos por usuário e ciclo de vida de sessões) representam o ativo mais importante do software.

No modelo tradicional em camadas acopladas ao framework (onde Controllers ou Services manipulam diretamente entidades `@Entity` do JPA/Hibernate e repositórios do Spring Data), surgem três problemas recorrentes à medida que o sistema cresce:
1. **Poluição do Domínio:** Entidades de negócio ficam repletas de anotações de persistência (`@Entity`, `@Table`, `@Column`) e serialização (`@JsonIgnore`), além de exigirem construtores vazios e `setters` mutáveis que enfraquecem as invariantes de domínio.
2. **Alto Acoplamento à Infraestrutura:** Trocar ou combinar mecanismos técnicos (como adicionar Cache L1 com Caffeine ou alternar Rate Limit entre Memória e Redis) exige alterar classes de regra de negócio.
3. **Erosão Arquitetural:** Sem barreiras automatizadas, atalhos de código acabam fazendo casos de uso dependerem diretamente de detalhes técnicos.

---

## 2. Opções Consideradas

1. **Arquitetura em Camadas Tradicional (MVC / Service + JPA Entity direta):**
   - *Prós:* Menos classes iniciais (sem necessidade de mappers entre domínio e persistência).
   - *Contras:* Forte acoplamento entre regras de negócio e banco de dados; difícil evolução para múltiplos adaptadores.
2. **Clean Architecture combinada com Arquitetura Hexagonal (Ports & Adapters) em 4 Camadas Canônicas:**
   - *Prós:* Domínio 100% puro (Java puro sem Spring/JPA); inversão de dependência explícita via Portas (`*Port`) e Adaptadores (`*Adapter`); facilidade para plugar cache e novos mecanismos de infraestrutura sem tocar nos casos de uso.
   - *Contras:* Exige mapeamento explícito entre modelos de domínio (`User`, `Movement`, `AuthSession`) e entidades JPA (`UserEntity`, `MovementEntity`, `AuthSessionEntity`).

---

## 3. Decisão Tomada

Adotamos **Clean Architecture combinada com Arquitetura Hexagonal (Ports & Adapters)**, organizando a raiz do pacote `peres.lucas.apifinanceira` estritamente em **4 camadas canônicas**:

```text
peres.lucas.apifinanceira
├── domain/                  # Camada 1: Núcleo de Negócio Puro
│   ├── model/               # User, Movement, MovementType, MovementTotals, AuthSession
│   ├── port/                # UserRepositoryPort, MovementRepositoryPort, AuthSessionRepositoryPort, RateLimitPort
│   └── exception/           # Exceções de domínio puras
├── application/             # Camada 2: Casos de Uso e Orquestração
│   ├── usecase/             # UserUseCase, MovementUseCase, AuthUseCase, SessionTokens
│   └── dto/                 # Contratos de entrada/saída (Requests/Responses)
├── adapter/                 # Camada 3: Adaptadores de Entrada (Driving Adapters)
│   └── web/
│       ├── controller/      # AuthController, UserController, MovementController (/api/v1/*)
│       └── exception/       # GlobalExceptionHandler, ErrorMessage
└── infrastructure/          # Camada 4: Adaptadores de Saída e Detalhes Técnicos (Driven Adapters)
    ├── config/              # ApiPaths, CacheConfig, CorrelationIdFilter, OpenApiConfig
    ├── persistence/
    │   ├── adapter/         # UserPersistenceAdapter, MovementPersistenceAdapter, AuthSessionPersistenceAdapter
    │   ├── entity/          # UserEntity, MovementEntity, AuthSessionEntity
    │   └── repository/      # UserJpaRepository, MovementJpaRepository, AuthSessionJpaRepository
    └── security/            # SecurityConfig, JwtUtil, RateLimitFilter, InMemoryRateLimitAdapter, RedisRateLimitAdapter
```

### Convenção de Nomenclatura Padronizada
- **Portas de Saída (`domain.port`):** Sufixo `*Port` (ex.: `UserRepositoryPort`, `RateLimitPort`).
- **Casos de Uso (`application.usecase`):** Sufixo `*UseCase` (ex.: `MovementUseCase`).
- **Adaptadores de Persistência (`infrastructure.persistence.adapter`):** Sufixo `*PersistenceAdapter`.
- **Entidades e Repositórios do Banco (`infrastructure.persistence`):** Sufixos `*Entity` e `*JpaRepository`.

---

## 4. Consequências e Trade-offs

### ✅ Pontos Positivos (Ganhos)
- **Pureza do Domínio:** As classes em `domain.model` usam apenas tipos padrão do Java (`BigDecimal`, `LocalDate`, `LocalDateTime`, `UUID`), podendo ser testadas em milissegundos sem subir contexto Spring.
- **Flexibilidade de Infraestrutura (Open-Closed Principle):** Conseguimos adicionar cache com Caffeine no `UserPersistenceAdapter` e suporte a Redis com Script Lua no `RedisRateLimitAdapter` sem alterar uma única linha dos `UseCases` ou do `Domain`.

### ⚠️ Pontos de Atenção / Custos Aceitos (Trade-offs)
- **Boilerplate de Mapeamento:** Os `*PersistenceAdapter` precisam converter `*Entity` $\leftrightarrow$ Modelo de Domínio (`toDomain` / `toEntity`). Esse custo foi aceito conscientemente porque garante que alterações no banco ou anotações do Hibernate jamais vazem para a regra de negócio.

---

## 5. Validação e Governança (Fitness Functions / Testes)

A arquitetura é protegida automaticamente a cada build pela *Fitness Function* [`CleanArchitectureTest.java`](../../api-financas/src/test/java/peres/lucas/apifinanceira/CleanArchitectureTest.java), que falha o build no CI caso:
1. Seja criado qualquer pacote fora dos 4 pacotes raiz (`domain`, `application`, `adapter`, `infrastructure`).
2. Qualquer classe em `domain` importe pacotes de `org.springframework.*`, `jakarta.persistence.*`, `org.hibernate.*`, `com.fasterxml.jackson.*` ou das camadas externas.
3. Qualquer classe em `domain` utilize anotações de frameworks.
4. Qualquer classe em `application.usecase` deixe de terminar com `UseCase` ou injete diretamente classes `*JpaRepository` ou `*Entity`.
