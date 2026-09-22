# À Tona — finanças para a vida como ela é

![Status do Projeto](https://img.shields.io/badge/status-em_desenvolvimento-blue)
![Angular](https://img.shields.io/badge/Front--End-Angular%20%7C%20TypeScript-red)
![Spring Boot](https://img.shields.io/badge/Back--End-Spring%20Boot%20%7C%20Java-success)
![PostgreSQL](https://img.shields.io/badge/Banco%20de%20Dados-PostgreSQL-blue)

À Tona é a identidade do projeto de controle financeiro pessoal. A aplicação web responsiva reúne uma página institucional, acesso à conta, gerenciamento de movimentações e resumo financeiro mensal. O site é integrado a uma API REST e a um banco de dados PostgreSQL.

---

## Objetivos do Projeto

- **Resolver uma dor real:** Facilitar o controle de despesas, receitas, planejamento de metas e tomada de decisões de compras em um único lugar.
- **Consolidação Técnica:** Aplicar conceitos de arquitetura cliente-servidor, segurança em APIs Java (JWT/BCrypt), desenvolvimento web responsivo e gerenciamento de infraestrutura em nuvem.

---

## Fluxo de Telas e UX

O website é responsivo, com navegação em desktop e dispositivos móveis. Na Onda 1, o primeiro acesso apresenta um estado vazio orientado à criação do primeiro registro, sem valores financeiros fictícios. O onboarding financeiro fica na Onda 2.

1. **Apresentação:** Landing page pública com a proposta da marca À Tona e a distinção entre recursos disponíveis e planejados.
2. **Autenticação:** Telas de Login (com opção *Remember Me*) e Cadastro com validação de senha.
3. **Primeiro acesso:** Resumo com orientação para cadastrar a primeira movimentação. O onboarding de Renda Base e Gastos Recorrentes entra na Onda 2.
4. **Movimentações:** Consulta por mês, filtros por tipo, totais do período, cadastro e edição no mesmo modal, além de exclusão com confirmação. Alterar a data de um registro leva a lista ao mês de destino. Os totais dessa tela representam somente o mês selecionado.
5. **Resumo (`/inicio`):** Saldo acumulado dos registros até o período selecionado, saldo anterior ao mês, entradas, saídas, resultado mensal e até cinco registros recentes desse mês. A navegação para Movimentações mantém o período; o botão de novo registro abre o formulário. O saldo é calculado pelos registros realizados, não sincronizado com bancos.

### Padrão visual da área interna

 A landing page e as telas de acesso mantêm a identidade editorial da marca.
A área autenticada utiliza uma sidebar fixa em desktop e uma barra compacta em celular,
inspirada na clareza de aplicativos bancários: saldo em primeiro plano, ações rápidas,
indicadores agrupados e extrato de leitura direta. Títulos compactos, fundo neutro,
superfícies brancas, bordas discretas e cores da marca organizam ações e estados.
Resumo e Movimentações compartilham essa navegação responsiva.
Há estados explícitos de carregamento, erro, ausência de registros e confirmação.
O cadastro valida valores em reais, campos obrigatórios e datas até o dia atual.

A revisão de Movimentações adota a referência de um livro-caixa contemporâneo:
período em faixa lateral (cabeçalho no celular), fundo marfim, linhas de extrato,
valores monoespaçados e destaques em coral e lavanda. A tipografia serifada fica
restrita a detalhes de identificação e ao formulário. O seletor de período é
um componente reutilizável, com navegação por teclado e indicação do mês atual.
O padrão aprovado foi expandido ao dashboard, aos fluxos de edição e exclusão,
mantendo a landing page e as telas de acesso sem alterações nesta entrega.

Os novos modais devem reutilizar `ModalComponent`, já integrado a Movimentações,
com cabeçalho e ações padronizados, foco acessível, rolagem interna e bloqueio
durante envio. O uso, as opções centralizada/lateral e exemplos estão no
[guia do modal](web-financas/src/app/shared/modal/README.md).

Consulta, cadastro, edição, exclusão e dashboard estão implementados. Os testes
automatizados de integração usam banco H2 isolado, sem alterar o PostgreSQL do
usuário. A conferência visual desta expansão no navegador permanece para a
avaliação do usuário; não foi executada uma nova rodada de testes visuais.

---

# Arquitetura e Stack Tecnológica

O ecossistema do projeto é completamente desacoplado, seguindo o modelo:
`Website (Angular) <-> HTTPS/JSON <-> API REST (Spring Boot) <-> DB (PostgreSQL)`

- **Front-end (Web):** Angular + TypeScript + SCSS, com layout responsivo.
- **Back-end (API REST):** Spring Boot (Java 17+), Spring Security, Spring Data JPA.
- **Banco de Dados:** PostgreSQL (Hospedado em nuvem via Supabase/Neon).

---

## Roadmap de Funcionalidades (Matriz MoSCoW)

O desenvolvimento foi dividido de forma incremental em 4 ondas principais:

### Onda 1: Essencial (Must Have)
- [x] **Autenticação Segura:** Cadastro e login com senhas criptografadas via **BCrypt**, tokens **JWT** e integração com o website.
- [x] **Fluxo de Caixa Base:** CRUD de Entradas (Receitas) e Saídas (Despesas).
- [x] **Dashboard Resumo:** Saldo acumulado dos registros, resultado por mês e histórico recente do período.

### Onda 2: Importante (Should Have)
- [ ] **Gestão de Renda e Cartões:** CRUD para limites de cartões de crédito e configuração de rendas mensais.
- [ ] **Painel de Dívidas:** Monitorização de pendências financeiras e progresso de parcelamentos.
- [ ] **Gastos Recorrentes:** Agendamento automático ou facilitação de despesas fixas (assinaturas, mensalidades).

### Onda 3: Desejável (Could Have)
- [ ] **Gráfico de Comparação Mensal:** Relatório visual dos últimos 12 meses (utilizando `Chart.js`).
- [ ] **O Cofrinho:** Sistema de metas de poupança isoladas (ex: Reserva de Emergência).
- [ ] **Lista de Desejos:** Cadastro de bens de consumo pretendidos com pesos de prioridade.

### Onda 4: Diferencial (Would Have)
- [ ] **Motor de Recomendação de Compra:** Algoritmo no Back-end que analisa a renda livre líquida do mês corrente e emite um parecer automatizado se o usuário deve ou não comprar um item da sua Lista de Desejos.

---

## 🗄️ Estrutura do Banco de Dados (Modelagem Relacional)

O banco de dados utiliza chaves do tipo **UUID** para evitar enumeração de recursos expostos pela API. Todas as tabelas são vinculadas à tabela pai de usuários:

- `tb_usuarios`: Armazena dados cadastrais e o hash da senha.
- `tb_movimentacoes`: Registro do fluxo de caixa diário (Entradas/Saídas).
- `tb_rendas`: Configuração das fontes de receita do usuário.
- `tb_cartoes_credito`: Dados de limites e vencimentos.
- `tb_dividas`: Registo de credores e parcelas.
- `tb_gastos_recorrentes`: Agendamentos de despesas fixas.

---

## Como executar o projeto

### Pré-requisitos
- Node.js (LTS)
- Java JDK 17+
- PostgreSQL

### API
```bash
cd ../api-financeira
# Configure as variáveis descritas em .env.example na IDE ou no terminal.
./mvnw spring-boot:run
```

### Website Angular
```bash
cd web-financas
npm install
npm start
```

### Diretrizes do Website

- O layout deve funcionar bem em desktop e celular, sem dependência de empacotamento nativo.
- A autenticação usa JWT curto em cookie HttpOnly, refresh token rotativo e proteção CSRF.
- As telas implementadas são landing page, cadastro, login, resumo e movimentações completas.
- Reinicie a API para disponibilizar resumo, edição e exclusão. Não há migração adicional nesta entrega.
- Após a avaliação desta expansão, o próximo escopo é a Onda 2: rendas, cartões, dívidas e gastos recorrentes.
- O front-end deve consumir a API por HTTPS em produção.
