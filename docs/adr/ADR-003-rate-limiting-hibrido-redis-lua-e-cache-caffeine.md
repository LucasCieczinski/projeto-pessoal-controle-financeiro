# ADR-003: Rate Limiting Híbrido (In-Memory vs. Redis Lua Script) e Cache L1 com Caffeine

- **Status:** Aceito
- **Data:** 2026-11-15
- **Autor(es):** Lucas Cieczinski Peres
- **Escopo:** Infraestrutura, Segurança e Performance (`api-financas`)

---

## 1. Contexto e Problema

O projeto precisava resolver dois desafios clássicos de engenharia de backend simultaneamente, respeitando uma restrição importante de ambiente de desenvolvimento: **o projeto deve rodar e passar em 100% dos testes localmente sem exigir Docker ou servidores externos instalados na máquina do desenvolvedor**.

1. **Desafio 1 — Proteção contra Força Bruta (Rate Limiting):** Rotas sensíveis (`/api/v1/auth/login`, `/api/v1/users`, `/api/v1/auth/refresh`) precisam limitar tentativas por IP/sessão. Em ambiente produtivo com múltiplas instâncias, o contador precisa ser compartilhado e atômico (Redis); em ambiente local/testes, precisa funcionar em memória sem dependências externas.
2. **Desafio 2 — Latência de Consultas Repetitivas de Usuário (Caching):** Praticamente todos os casos de uso autenticados (`UserUseCase`, `MovementUseCase`, `AuthUseCase`) consultam `userRepositoryPort.findByEmail(email)`. Ir ao banco relacional repetidamente para ler dados cadastrais que raramente mudam gera *overhead* desnecessário de I/O.

---

## 2. Opções Consideradas

### Para o Rate Limiting:
1. **Apenas `ConcurrentHashMap` em Memória:** Simples e sem dependências, mas não compartilha estado caso a API escale horizontalmente para 2+ instâncias.
2. **Apenas Redis Obrigatório:** Resolve escala horizontal, mas quebra a execução local e os testes automatizados quando não há um servidor Redis ativo.
3. **Padrão Strategy via Ports & Adapters (`RateLimitPort`) + `@ConditionalOnProperty`:** Mantém `InMemoryRateLimitAdapter` como padrão local (`matchIfMissing = true`) e permite ativar `RedisRateLimitAdapter` via configuração (`app.rate-limit.storage=redis`).

### Para o Cache de Usuários:
1. **Cache Distribuído (Redis) para tudo:** Adiciona latência de rede (~1-3ms) e serialização para objetos pequenos e muito consultados.
2. **Cache L1 In-Memory de Alta Performance com Caffeine:** Acesso em nanossegundos direto na Heap da JVM, com política de expiração (`TTL`) e limite máximo de entradas (`W-TinyLFU`).

---

## 3. Decisão Tomada

### 3.1. Rate Limiting com `RateLimitPort` e Script Lua Atômico no Redis
Definimos a porta de domínio [`RateLimitPort`](../../api-financas/src/main/java/peres/lucas/apifinanceira/domain/port/RateLimitPort.java) e duas implementações na camada de infraestrutura selecionadas pelo [`RateLimitConfig`](../../api-financas/src/main/java/peres/lucas/apifinanceira/infrastructure/security/RateLimitConfig.java):

- **`InMemoryRateLimitAdapter` (`app.rate-limit.storage=memory`, padrão):** Utiliza `ConcurrentHashMap` e limpeza periódica de janelas expiradas.
- **`RedisRateLimitAdapter` (`app.rate-limit.storage=redis`):** Executa um **Script Lua atômico** dentro do Redis (`DefaultRedisScript<Long>`):
  ```lua
  local current = redis.call('INCR', KEYS[1])
  if current == 1 then
      redis.call('PEXPIRE', KEYS[1], ARGV[1])
  end
  return current
  ```
  > **Por que Script Lua?** Executar `INCR` e `PEXPIRE` em duas chamadas separadas do Java criaria uma *Race Condition* (se a JVM caísse entre o `INCR` e o `PEXPIRE`, a chave ficaria presa no Redis com valor `1` e sem TTL para sempre, bloqueando o usuário permanentemente). Como o Redis é *single-threaded* na execução de scripts Lua, o bloco inteiro roda de forma 100% atômica em uma única viagem de rede.

### 3.2. Cache L1 com Caffeine no `UserPersistenceAdapter`
Configuramos o [`CacheConfig`](../../api-financas/src/main/java/peres/lucas/apifinanceira/infrastructure/config/CacheConfig.java) com `CaffeineCacheManager` (`maximumSize = 1000`, `expireAfterWrite = 10 minutos`, `.recordStats()` integrado ao Micrometer/Actuator) e aplicamos o cache diretamente no adaptador de persistência [`UserPersistenceAdapter`](../../api-financas/src/main/java/peres/lucas/apifinanceira/infrastructure/persistence/adapter/UserPersistenceAdapter.java):
- **Leitura (`findByEmail`):** `@Cacheable(value = CacheConfig.USER_BY_EMAIL_CACHE, key = "#email", unless = "#result == null")` — evitando *Negative Caching* (não armazena `Optional.empty()` na memória).
- **Escrita (`save`):** `@CacheEvict(value = CacheConfig.USER_BY_EMAIL_CACHE, key = "#user.email")` — garantindo invalidação imediata quando os dados do usuário sofrem alteração.

---

## 4. Consequências e Trade-offs

### ✅ Pontos Positivos (Ganhos)
- **Developer Experience (DX) Zero-Config:** Qualquer desenvolvedor ou avaliador clona o repositório e roda `./mvnw test` sem precisar subir containers Docker.
- **Transparência para a Camada de Aplicação:** Ao posicionar o `@Cacheable` no `UserPersistenceAdapter`, todos os casos de uso (`UserUseCase`, `MovementUseCase`, `AuthUseCase`) foram acelerados simultaneamente sem poluir a camada `application`.

### ⚠️ Pontos de Atenção / Custos Aceitos (Trade-offs)
- **Coerência de Cache L1 em Cluster:** Como o Caffeine fica na memória RAM de cada instância, em um cenário futuro com múltiplas instâncias da API, uma atualização de perfil (`save`) invalida imediatamente o cache da instância que processou a escrita, enquanto as demais instâncias convergem em no máximo 10 minutos (TTL). Para dados cadastrais básicos (`User`), essa consistência eventual curta é ideal.

---

## 5. Validação e Governança (Fitness Functions / Testes)

- [`InMemoryRateLimitAdapterTest.java`](../../api-financas/src/test/java/peres/lucas/apifinanceira/infrastructure/security/InMemoryRateLimitAdapterTest.java): Valida contagem, bloqueio e expiração de janela em memória.
- [`RedisRateLimitAdapterTest.java`](../../api-financas/src/test/java/peres/lucas/apifinanceira/infrastructure/security/RedisRateLimitAdapterTest.java): Valida via Mockito a execução do script Lua, prefixos de chave e limites de bloqueio sem depender de instância Redis ativa.
