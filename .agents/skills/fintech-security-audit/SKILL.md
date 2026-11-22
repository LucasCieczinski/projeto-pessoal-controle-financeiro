---
name: fintech-security-audit
description: >-
  Realiza auditoria de segurança bancária (AppSec), conformidade arquitetural (Clean Architecture),
  precisão contábil (BigDecimal) e qualidade de testes (JaCoCo/Vitest) no código do À Tona.
---

# Auditoria de Segurança Bancária e Arquitetura (`À Tona`)

Execute este checklist após implementar novas rotas, casos de uso, componentes Angular ou futuras integrações de IA/Tools no projeto.

## 1. Auditoria Anti-IDOR e Isolamento de Dados (Crítico em Bancos/Fintechs)

1. Inspecione todos os `*Controller.java` em `api-financas/src/main/java/peres/lucas/apifinanceira/adapter/web/controller/`:
   - Confirme que nenhum endpoint aceita `usuarioId`, `userId` ou `email` via `@RequestBody`, `@PathVariable` ou `@RequestParam` para identificar o dono de recursos financeiros.
   - Confirme que a identidade vem exclusivamente de `java.security.Principal` (`principal.getName()`).
2. Inspecione todos os `*UseCase.java` e `*RepositoryPort.java`:
   - Confirme que operações por ID (`findById`, `update`, `delete`, `patch`) usam `findByIdAndUserId(id, userId)` — nunca `findById(id)` sem validar o dono.
   - Confirme que acesso cruzado entre dois usuários retorna `404 Not Found` (`*NotFoundException` tratada no `GlobalExceptionHandler`), evitando enumeração de IDs.
3. Inspecione os `*FlowIntegrationTest.java`:
   - Garanta que exista teste explícito criando dois usuários distintos (`ana` e `bia` ou `owner` e `other`) e verificando `404` nas tentativas de leitura/alteração/exclusão cruzada.

## 2. Auditoria de Sessão, CSRF e Segurança Web

1. **Backend (`SecurityConfig.java` e `ApiPaths.java`):**
   - Verifique se novas rotas mutáveis (`POST`, `PUT`, `PATCH`, `DELETE`) não foram adicionadas indevidamente a `ignoringRequestMatchers(...)` do CSRF nem a `permitAll()`.
   - Verifique que todas as rotas usam constantes de `ApiPaths`.
2. **Frontend (`web-financas/src/app/`):**
   - Busque chamadas `this.http.post`, `this.http.put`, `this.http.patch`, `this.http.delete` nos serviços em `core/`: todas devem estar encapsuladas em `this.authService.ensureCsrf().pipe(switchMap(...))`.
   - Confirme que não há uso de `localStorage` ou `sessionStorage` para armazenar credenciais ou tokens.

## 3. Auditoria Contábil e de Domínio

1. **Precisão monetária:** verifique se `domain/model/`, `application/dto/` e `infrastructure/persistence/entity/` usam `java.math.BigDecimal` (zero `double`/`float`).
2. **Higienização e invariantes:** verifique se modelos de domínio aplicam `.trim()` em textos e validam limites (`> 0`, `>= 0`, `1..31`, `parcelasPagas <= totalParcelas`) em sincronia com os `CHECK` constraints das migrations Flyway (`V1` e `V2`).
3. **Separação Realizado vs. Planejamento:** garanta que `Income`, `RecurringExpense`, `Debt` e `CreditCard` não alteram o saldo de `Movement` sem movimentação explícita.

## 4. Auditoria Arquitetural e Quality Gates

1. Inspecione `api-financas/src/test/java/peres/lucas/apifinanceira/CleanArchitectureTest.java`:
   - Confirme que todas as classes novas de `domain.model`, `domain.port` e `application.usecase` estão listadas em `domainClasses` e `applicationUseCases`.
2. Execute os gates automatizados:
   - Backend (inclui `CleanArchitectureTest` e verificação JaCoCo >= 80%):
     ```bash
     ./mvnw verify
     ```
     *(No Windows PowerShell: `.\mvnw.cmd verify`)*
   - Frontend (Vitest + compilação):
     ```bash
     npm test -- --watch=false
     npm run build
     ```
3. Se encontrar qualquer lacuna de teste ou vulnerabilidade, adicione primeiro o teste de regressão correspondente e depois corrija a implementação.
