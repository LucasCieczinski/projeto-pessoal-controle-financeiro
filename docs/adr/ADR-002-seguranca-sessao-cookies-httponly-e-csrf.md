# ADR-002: Arquitetura de Sessão Web Segura com Cookies HttpOnly, Refresh Token Rotation e CSRF

- **Status:** Aceito
- **Data:** 2026-11-15
- **Autor(es):** Lucas Cieczinski Peres
- **Escopo:** Segurança (Backend `api-financas` & Frontend `web-financas`)

---

## 1. Contexto e Problema

Aplicações SPA (Single Page Applications em Angular/React) frequentemente adotam o padrão simplista de receber um token JWT no corpo do login e salvá-lo no `localStorage` ou `sessionStorage` do navegador.

Embora simples de implementar, esse modelo possui duas falhas graves para uma aplicação financeira:
1. **Vulnerabilidade a XSS (Cross-Site Scripting):** Qualquer script malicioso ou dependência comprometida rodando na página tem acesso total a `window.localStorage`, podendo exfiltrar o JWT do usuário.
2. **Impossibilidade de Revogação Imediata (JWT Puro Stateless):** Se um token de longa duração é roubado, o servidor não consegue invalidá-lo antes de sua data de expiração sem manter estado de sessão.

---

## 2. Opções Consideradas

1. **JWT no `localStorage` enviado via header `Authorization: Bearer`:**
   - *Prós:* Não requer proteção CSRF nem configuração de cookies cross-origin.
   - *Contras:* Totalmente exposto a leitura via JavaScript (XSS). Rejeitado por motivos de segurança.
2. **Sessão Híbrida com Cookies `HttpOnly` + Refresh Token Persistido e Rotativo + Proteção CSRF:**
   - *Prós:* Tokens de autenticação ficam invisíveis para o JavaScript do navegador (`HttpOnly`); sessões podem ser revogadas instantaneamente no banco; tokens de acesso têm vida curta (15 min).
   - *Contras:* Como o navegador envia cookies automaticamente, torna-se obrigatório implementar proteção anti-CSRF e configurar CORS com `allowCredentials: true`.

---

## 3. Decisão Tomada

Implementamos uma arquitetura de autenticação **Defesa em Profundidade (Defense in Depth)** baseada em:

1. **Access Token JWT Curto em Cookie `HttpOnly` (`atona_access`):**
   - Expiração padrão curta (15 minutos), assinado via HMAC-SHA, contendo o identificador da sessão (`sid`) e o `subject` (e-mail).
   - O `JwtAuthenticationFilter` lê exclusivamente o cookie `atona_access` (ignorando headers `Authorization: Bearer` arbitrários) e verifica se a sessão vinculada continua ativa.
2. **Refresh Token Opaco e Rotativo em Cookie `HttpOnly`Restrito (`atona_refresh`):**
   - Possui escopo de caminho restrito (`Path=/api/v1/auth`), sendo enviado pelo navegador apenas para as rotas de autenticação (`/refresh` e `/logout`), jamais trafegando nas chamadas comuns de negócio (`/api/v1/movimentacoes`).
   - No banco de dados (`tb_auth_sessions` / `AuthSession`), **nunca salvamos o token em texto puro**, apenas seu hash **SHA-256** (`token_hash`).
   - A cada chamada ao `POST /api/v1/auth/refresh`, o token atual é rotacionado por um novo hash e novos cookies são emitidos.
3. **Proteção CSRF Adequada para SPA (`CookieCsrfTokenRepository`):**
   - O cookie `XSRF-TOKEN` é emitido com `HttpOnly=false` para que o Angular possa lê-lo e enviá-lo no cabeçalho `X-XSRF-TOKEN` em todas as requisições mutáveis (`POST`, `PUT`, `DELETE`).
4. **Hardening de Cabeçalhos HTTP (`SecurityConfig`):**
   - Aplicação estrita de `Content-Security-Policy (CSP)`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin` e `Strict-Transport-Security (HSTS)`.

---

## 4. Consequências e Trade-offs

### ✅ Pontos Positivos (Ganhos)
- **Imunidade de Tokens a XSS:** Nenhum código JavaScript no frontend consegue ler `atona_access` ou `atona_refresh`.
- **Revogação Real no Logout:** Ao chamar `POST /api/v1/auth/logout`, a sessão é marcada como revogada no banco (`revoked_at`) e os cookies são limpos com `Max-Age=0`.

### ⚠️ Pontos de Atenção / Custos Aceitos (Trade-offs)
- **Coordenação de Renovação no Frontend:** O interceptor HTTP do Angular (`auth.interceptor.ts`) precisa interceptar erros `401 Unauthorized`, acionar silenciosamente `/api/v1/auth/csrf` + `/api/v1/auth/refresh` e repetir a requisição original sem desconectar o usuário.

---

## 5. Validação e Governança (Fitness Functions / Testes)

- [`AuthFlowIntegrationTest.java`](../../api-financas/src/test/java/peres/lucas/apifinanceira/AuthFlowIntegrationTest.java): Valida o fluxo completo de cadastro, emissão de cookies `HttpOnly`, rejeição de requests sem CSRF (`403 Forbidden`), rotação de refresh token (invalidando o refresh token anterior) e revogação pós-logout.
- [`AuthControllerTest.java`](../../api-financas/src/test/java/peres/lucas/apifinanceira/adapter/web/controller/AuthControllerTest.java): Valida os headers de segurança HTTP e o bloqueio das rotas do Swagger quando `app.swagger.enabled=false`.
