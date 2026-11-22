# Regras do Frontend Angular — `web-financas` (Angular 22 + Vitest)

Este módulo implementa a aplicação web responsiva do **À Tona** com **Standalone Components**, **Signals**, **Reactive Forms** e testes unitários em **Vitest**.

## 1. Estrutura de Diretórios (`src/app/`)

- **`core/`**: serviços singleton (`@Injectable({ providedIn: 'root' })`), modelos/interfaces TypeScript (`*.models.ts`), interceptors funcionais e guardas de rota (`authGuard`).
  - Ex.: `core/auth/`, `core/errors/`, `core/movements/`. Novos domínios da Onda 2 devem seguir a mesma organização (ex.: `core/planning/` ou `core/debts/`, `core/credit-cards/`).
- **`features/`**: componentes de página/rota (Standalone Components com `*.component.ts`, `*.component.html`, `*.component.scss`, `*.component.spec.ts`), carregados via lazy loading em `app.routes.ts`.
- **`shared/`**: componentes e estilos compartilhados obrigatórios:
  - `PrivateHeaderComponent` (`app-private-header`): navegação principal autenticada (sidebar desktop + bottom nav mobile).
  - `PeriodNavigationComponent` (`app-period-navigation`): navegação de competência mensal.
  - `ModalComponent` (`app-modal`): diálogos acessíveis controlados por `signal(false)`.
  - Estilos compartilhados: `_page-shell.scss` e `_auth-form.scss`.

## 2. Segurança de Sessão e CSRF

- **Sem Tokens no Browser Storage:** a autenticação usa cookies `HttpOnly` enviados automaticamente pelo navegador (`auth.interceptor.ts` trata credenciais e renovação em `401`). Nunca persista tokens em `localStorage` ou `sessionStorage`.
- **CSRF Obrigatório em Mutações:** todo método de serviço que executa `POST`, `PUT`, `PATCH` ou `DELETE` **deve** chamar `this.authService.ensureCsrf()` antes da requisição HTTP:
  ```ts
  return this.authService.ensureCsrf().pipe(
    switchMap(() => this.http.post<Response>(`${this.apiUrl}/recurso`, request)),
  );
  ```

## 3. Padrões de Código Angular 22

- **Injeção de dependências:** prefira `inject(...)` em propriedades `private readonly` em vez de injeção via construtor.
- **Signals & Lifecycle:**
  - Use `signal(...)`, `input(...)`, `output(...)` e `viewChild(...)` para estado local e comunicação entre componentes.
  - Use `takeUntilDestroyed(this.destroyRef)` em todas as inscrições (`subscribe`) de observables HTTP e rotas.
  - Utilize `ChangeDetectorRef.markForCheck()` após respostas assíncronas quando o componente gerencia flags de estado simples.
- **Formulários e Validação Financeira:**
  - Use `NonNullableFormBuilder` com validadores alinhados às constraints do backend (ex.: tamanho máximo, valores positivos).
  - Valide e converta valores monetários em centavos inteiros (`parseAmount`), aceitando formato `pt-BR` (`1.234,56`) e formatando com `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`.
- **Tratamento de Erros (`ApiError`):**
  - Em caso de status `401`, redirecione para `/login`.
  - Extraia mensagens amigáveis de `error.error as ApiError` priorizando `Object.values(apiError?.campos ?? {})[0] ?? apiError?.mensagem`.
- **Rotas Protegidas:**
  - Toda nova rota privada em `app.routes.ts` deve usar `canActivate: [authGuard]` e lazy loading (`loadComponent`).

## 4. Testes e Verificação (Vitest)

- Todo novo service e component deve possuir arquivo `*.spec.ts` cobrindo: chamadas HTTP e `ensureCsrf()`, renderização básica, validação de formulário, tratamento de erro (`400`/`401`/`404`/`0`) e fluxos principais de criação/edição/exclusão.
- **Quality Gate obrigatório:**
  ```bash
  npm test -- --watch=false
  ```
  O build de produção (`npm run build`) também deve compilar sem erros de TypeScript estrito.
