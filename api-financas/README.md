# API Financeira

API Spring Boot para o projeto de controle financeiro pessoal.

## Configuração local

Defina as variáveis documentadas em `.env.example` na configuração da IDE ou no
terminal antes de iniciar a aplicação. O arquivo `.env` é ignorado pelo Git e não é
carregado automaticamente pelo Spring Boot.

A variável `JWT_SECRET` deve conter uma chave aleatória codificada em Base64 com pelo
menos 32 bytes. Nunca reutilize o valor que existia nas versões anteriores do projeto.

O Flyway executa `V1`, `V1.1`, `V2` e `V3` automaticamente em um banco vazio.
No banco local inspecionado em 16/09/2026 havia apenas `tb_usuarios`. Uma execução
anterior registrou **baseline 1**, mas `V2` falhou porque as demais tabelas não
existiam. A migração corretiva `V1.1` cria essas tabelas antes de `V2`, sem remover
usuários nem editar o histórico. Nesse banco, deixe `FLYWAY_BASELINE_ON_MIGRATE=false`
e reinicie a API após fazer backup. Não apague `flyway_schema_history` nem repita o
baseline.

Para **outro** banco parcial sem histórico Flyway e com somente `tb_usuarios`
compatível, faça backup e use `FLYWAY_BASELINE_ON_MIGRATE=true` com
`FLYWAY_BASELINE_VERSION=0` apenas na primeira execução. Se o esquema já corresponder
integralmente à `V1`, use versão `1`; se `V2` também estiver aplicada, use versão `2`.
Depois volte `FLYWAY_BASELINE_ON_MIGRATE=false`. Não execute o baseline sem conferir
a estrutura: uma versão incorreta pode tentar repetir alterações ou omiti-las.

## Autenticação

- `POST /users` cria a conta; `POST /auth/login` cria a sessão e responde `204`, sem
  retornar tokens no JSON.
- O JWT de acesso fica no cookie HttpOnly `atona_access` (15 minutos por padrão). O
  refresh token aleatório fica no cookie HttpOnly `atona_refresh`; apenas seu hash
  é persistido no banco. `POST /auth/refresh` troca o refresh token por outro e
  emite um novo JWT. `POST /auth/logout` revoga a sessão e limpa os cookies.
- Sem a opção “Continuar conectado”, os cookies são de sessão do navegador e o
  refresh expira no servidor após 24 horas. Com a opção, duram até 30 dias. Os
  prazos podem ser configurados em `.env.example`.
- Requisições que alteram dados autenticados precisam enviar o cookie `XSRF-TOKEN`
  também no cabeçalho `X-XSRF-TOKEN`. `GET /auth/csrf` emite esse cookie. O frontend
  faz isso automaticamente.
- Em produção, use HTTPS, `COOKIE_SECURE=true` e publique frontend e API sob o
  mesmo site (por exemplo, frontend em `/` e API em `/api`). No desenvolvimento,
  use `localhost` nos dois endereços, mesmo que as portas sejam diferentes.

## Movimentações e resumo — Onda 1

- `GET /movimentacoes?mes=2026-09` consulta os registros do mês, do mais recente
  para o mais antigo, exclusivamente do usuário autenticado.
- `POST /movimentacoes` registra uma entrada ou saída realizada. Recebe
  `descricao`, `valor` (positivo, até duas casas decimais), `tipo` (`ENTRADA` ou
  `SAIDA`), `categoria` e `dataMovimentacao` (`AAAA-MM-DD`, até a data atual).
  Exige sessão e proteção CSRF; o usuário é determinado pela sessão, não pelo JSON.
- `PUT /movimentacoes/{id}` atualiza os mesmos campos do cadastro, com as mesmas
  validações. `DELETE /movimentacoes/{id}` exclui e retorna `204`. Ambos exigem
  CSRF e só acessam registros do usuário autenticado; IDs ausentes ou pertencentes
  a outra conta retornam `404`, sem revelar a existência de registros de terceiros.
- `GET /movimentacoes/resumo?mes=2026-09` retorna `mes`, `entradas`, `saidas`,
  `resultado` (entradas menos saídas do mês), `saldoAnterior`, `saldoAcumulado`,
  `quantidade` (registros do mês), `totalRegistros` (registros até o final do mês)
  e `ultimasMovimentacoes` (até cinco registros do mês, do mais recente ao mais
  antigo). Saldos podem ser negativos. Os cálculos usam BigDecimal e agregação no
  banco, sempre por usuário. Meses posteriores não entram no saldo do período.
- O frontend disponibiliza o CRUD em `/movimentacoes` e o resumo em `/inicio`.
  Uma conta vazia não recebe valores fictícios. O saldo acumulado considera somente
  movimentações realizadas; rendas previstas e despesas recorrentes não entram.
- Não há nova migração: os registros utilizam `tb_movimentacoes`, já prevista
  nas migrações existentes. Reinicie a API para carregar os novos endpoints.

## Execução

```bash
./mvnw spring-boot:run
```

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Testes

```bash
./mvnw test
```

Os testes que usam persistência executam com o perfil `test` e banco H2 em memória.
Eles não reutilizam as credenciais ou os dados do PostgreSQL.
As migrações de produção devem ser verificadas separadamente em um PostgreSQL de
teste antes da primeira inicialização em um banco existente.

## Segurança operacional

Credenciais presentes em commits anteriores devem ser rotacionadas no PostgreSQL e
no ambiente de hospedagem. Remover um valor do arquivo atual não o remove do histórico
Git; qualquer reescrita do histórico deve ser coordenada antes de um `push --force`.
