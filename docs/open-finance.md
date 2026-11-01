# Open Finance — estudo de integração futura

**Status:** não implementado. Fora do escopo das Ondas 1 a 4.

Este documento registra a avaliação inicial para importar dados bancários ao À Tona
no futuro. Ele não é um contrato de implementação e não altera o modelo atual: hoje o
saldo é calculado exclusivamente pelas movimentações cadastradas manualmente.

## Objetivo

Permitir que o usuário autorize a leitura de contas e transações bancárias para:

- reduzir o trabalho de lançar movimentações manualmente;
- conferir o que já foi registrado;
- sugerir lançamentos a partir de transações bancárias;
- manter o usuário no controle do que entra no saldo.

A integração inicial deve ser **somente leitura**. Iniciação de pagamentos (Pix) não
faz parte deste estudo.

## Por que não implementar diretamente agora

O Open Finance Brasil é regulado pelo Banco Central e usa um perfil de segurança
próprio, baseado em OAuth 2.0/FAPI. Uma integração direta como receptor de dados
normalmente envolve:

- participação ou autorização regulatória compatível com o papel exercido;
- certificados e autenticação mútua (mTLS) com cadeia ICP-Brasil;
- registro dinâmico de cliente (DCR) e gestão de certificados;
- fluxo de consentimento com escopo, finalidade, prazo e revogação;
- endpoints de consentimento, contas e transações;
- webhooks e tratamento de revogação/alteração de consentimento;
- requisitos de segurança, auditoria e LGPD.

Para um projeto pessoal, o caminho mais viável é avaliar um **provedor/agregador
autorizado** que exponha uma API de consentimento e sincronização. A decisão entre
integração direta e provedor deve ser tomada com validação jurídica e regulatória.

## Opções de arquitetura

### Opção A — Provedor autorizado (recomendada para o MVP)

O À Tona conversa com um provedor especializado. O provedor cuida da conexão com as
instituições financeiras e devolve contas e transações normalizadas.

Vantagens:

- menor tempo de implementação;
- menor exposição a mudanças regulatórias;
- fluxo de consentimento pronto;
- suporte a múltiplos bancos.

Riscos:

- dependência comercial do provedor;
- custo por conexão/usuário;
- necessidade de contrato e avaliação de LGPD;
- limite de instituições e escopos disponíveis.

### Opção B — Integração direta

O À Tona atua como participante/receptor e implementa o fluxo FAPI-BR completo.

Vantagens:

- controle total da experiência e dos dados;
- possibilidade de evoluir para outros produtos.

Riscos:

- alto custo de certificação, segurança e operação;
- necessidade de acompanhar continuamente as especificações;
- maior superfície de ataque e de conformidade.

**Decisão sugerida:** começar pela opção A em ambiente sandbox e reavaliar a opção B
somente se houver necessidade de produto ou volume que justifique.

## Fluxo funcional proposto

1. O usuário acessa **Configurações → Conexões bancárias** e escolhe conectar um banco.
2. O backend cria uma solicitação de consentimento no provedor.
3. O usuário é redirecionado para o ambiente da instituição financeira, autentica e
   autoriza os escopos solicitados.
4. O provedor retorna ao backend com o resultado do consentimento.
5. O backend valida o retorno, registra o consentimento e agenda a primeira sincronização.
6. Contas e transações são importadas como **sugestões pendentes**, não como
   movimentações realizadas.
7. O usuário revisa, categoriza e confirma cada sugestão.
8. A confirmação cria uma movimentação com origem identificada e vínculo com a
   transação externa, evitando duplicidade.
9. Se o usuário revogar o consentimento, a sincronização para e os dados vinculados
   são tratados conforme a política de retenção e a LGPD.

## Regras de produto

- Nenhuma transação importada altera o saldo antes da confirmação do usuário.
- O saldo do À Tona continua sendo o saldo dos registros confirmados, não o saldo
  bancário exibido pela instituição.
- Uma transação externa não pode gerar duas movimentações: usar identificador externo
  e chave de idempotência.
- Transferências entre contas do mesmo usuário não devem ser contadas como receita ou
  despesa; exigem tratamento específico.
- Estornos, transações pendentes e lançamentos futuros precisam de estado próprio.
- A desconexão deve ser simples e explícita.

## Modelo de dados proposto

Nenhuma tabela abaixo existe hoje. A implementação exigirá migrações Flyway novas.

### `tb_conexoes_open_finance`

- `id` UUID
- `usuario_id` UUID
- `provedor` VARCHAR
- `consentimento_externo_id` VARCHAR
- `status` VARCHAR (`PENDENTE`, `ATIVA`, `EXPIRADA`, `REVOGADA`, `ERRO`)
- `escopos` VARCHAR/TEXT
- `expira_em` TIMESTAMPTZ
- `criada_em` TIMESTAMPTZ
- `atualizada_em` TIMESTAMPTZ
- `revogada_em` TIMESTAMPTZ

### `tb_contas_bancarias`

- `id` UUID
- `usuario_id` UUID
- `conexao_id` UUID
- `instituicao` VARCHAR
- `tipo` VARCHAR
- `agencia_mascarada` VARCHAR
- `numero_mascarado` VARCHAR
- `apelido` VARCHAR
- `ativa` BOOLEAN

### `tb_importacoes_open_finance`

- `id` UUID
- `usuario_id` UUID
- `conta_id` UUID
- `transacao_externa_id` VARCHAR
- `data_transacao` DATE
- `valor` NUMERIC(12, 2)
- `descricao` VARCHAR
- `tipo` VARCHAR (`ENTRADA`, `SAIDA`, `TRANSFERENCIA`)
- `status` VARCHAR (`PENDENTE`, `CONFIRMADA`, `IGNORADA`, `DUPLICADA`)
- `movimentacao_id` UUID nullable
- `payload_hash` VARCHAR
- `criada_em` TIMESTAMPTZ

Restrição sugerida: unicidade em `(usuario_id, conta_id, transacao_externa_id)`.

### Evolução de `tb_movimentacoes`

Adicionar, se a integração for aprovada:

- `origem` VARCHAR (`MANUAL`, `OPEN_FINANCE`);
- `id_externo` VARCHAR nullable;
- `conta_bancaria_id` UUID nullable.

A migração deve preservar os registros existentes e preencher `origem = 'MANUAL'`.

## Segurança e LGPD

- Consentimento explícito, informado, com finalidade, escopo e prazo.
- Revogação a qualquer momento, com efeito na sincronização.
- Tokens e segredos criptografados em repouso; nunca no front-end.
- Credenciais bancárias jamais são recebidas ou armazenadas pela aplicação.
- Validação de assinatura/origem dos webhooks.
- Idempotência e proteção contra replay.
- Logs sem dados financeiros sensíveis ou tokens.
- Controle de acesso por usuário em todas as consultas.
- Política de retenção e exclusão de dados.
- Registro de auditoria das conexões, sincronizações e confirmações.
- Revisão jurídica e de privacidade antes de produção.

## Fases sugeridas

1. **Pesquisa e sandbox**
   - escolher provedor;
   - validar consentimento, escopos e qualidade dos dados;
   - testar contas, transações, paginação e revogação.

2. **Fundação no backend**
   - criar as migrações;
   - implementar o adaptador do provedor;
   - criar consentimento, callback, sincronização e webhook;
   - adicionar idempotência e testes de integração.

3. **Experiência no front-end**
   - tela de conexões bancárias;
   - fluxo de consentimento;
   - lista de sugestões;
   - confirmação, edição, ignorar e desfazer;
   - estados de erro, expiração e reconexão.

4. **Endurecimento**
   - auditoria, métricas e alertas;
   - testes de segurança;
   - LGPD, contratos e documentação operacional;
   - piloto limitado antes de liberar para todos.

## Decisões pendentes

- Provedor autorizado versus integração direta.
- Quais instituições e tipos de conta serão suportados no primeiro momento.
- Importar apenas transações recentes ou histórico completo.
- Confirmar manualmente todas as transações ou permitir regras de autoaprovação.
- Como tratar transferências entre contas, estornos e transações pendentes.
- Prazo de retenção após revogação do consentimento.
- Se cartões de crédito também serão importados no futuro.

## Referências

- Open Finance Brasil: <https://openfinancebrasil.org.br/>
- Área do desenvolvedor: <https://openfinancebrasil.atlassian.net/wiki/spaces/OF/overview>
- Banco Central — Open Finance: <https://www.bcb.gov.br/estabilidadefinanceira/openfinance>
