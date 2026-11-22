---
name: adr-writer
description: >-
  Cria ou atualiza Architecture Decision Records (ADRs) em docs/adr/ seguindo o template oficial
  ADR-000-template-padrao.md e atualiza o índice no README.md.
---

# Escrita de ADRs (`À Tona`)

Use esta skill sempre que uma decisão arquitetural relevante for tomada (ex.: adoção de Spring AI / LangChain4j, MCP Server, Open Finance, mensageria ou mudanças de persistência/segurança).

## Passo 1: Identificar Numeração e Ler Referências

1. Liste os arquivos em `projeto-pessoal-controle-financeiro/docs/adr/` para identificar o próximo número sequencial (`ADR-005`, `ADR-006`, etc.).
2. Leia `projeto-pessoal-controle-financeiro/docs/adr/ADR-000-template-padrao.md`.
3. Opcionalmente leia um ADR existente (ex.: `ADR-001-clean-architecture-e-ports-and-adapters.md`) para manter o mesmo nível técnico e tom de engenharia.

## Passo 2: Criar o Novo ADR

Crie `projeto-pessoal-controle-financeiro/docs/adr/ADR-<NNN>-<slug-kebab-case>.md` respeitando integralmente as 5 seções do template:

1. **Cabeçalho:** `Status` (`Proposto` ou `Aceito`), `Data` (`YYYY-MM-DD`), `Autor(es)` (`Lucas Cieczinski Peres`) e `Escopo`.
2. **1. Contexto e Problema:** cenário técnico, requisitos de negócio/segurança e restrições do ambiente local (ex.: rodar testes sem Docker usando H2/in-memory).
3. **2. Opções Consideradas:** ao menos 2 ou 3 alternativas reais com prós e contras técnicos objetivos.
4. **3. Decisão Tomada:** descreva exatamente como a solução se encaixa nas 4 camadas canônicas (`domain`, `application`, `adapter`, `infrastructure`) de `peres.lucas.apifinanceira` e/ou na estrutura `core`/`features`/`shared` do `web-financas`.
5. **4. Consequências e Trade-offs:** ganhos concretos e custos aceitos com estratégias de mitigação.
6. **5. Validação e Governança (Fitness Functions / Testes):** liste os testes unitários, de integração e regras de `CleanArchitectureTest` que garantem o cumprimento contínuo da decisão.

## Passo 3: Atualizar Índice no `README.md`

1. Abra `projeto-pessoal-controle-financeiro/README.md`.
2. Adicione uma nova linha na tabela da seção **Governança Arquitetural — ADRs (Architecture Decision Records)** apontando para `./docs/adr/ADR-<NNN>-<slug>.md` com o status correto.
3. Revise o link Markdown e a formatação da tabela antes de concluir.
