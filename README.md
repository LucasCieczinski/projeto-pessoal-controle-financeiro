# À Tona — finanças para a vida como ela é

![Status do Projeto](https://img.shields.io/badge/status-em_desenvolvimento-blue)
![Angular](https://img.shields.io/badge/Front--End-Angular%20%7C%20TypeScript-red)
![Spring Boot](https://img.shields.io/badge/Back--End-Spring%20Boot%20%7C%20Java-success)
![PostgreSQL](https://img.shields.io/badge/Banco%20de%20Dados-PostgreSQL-blue)

![Java](https://img.shields.io/badge/Java-17%2F21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)

## 📌 Contexto da Solução
A gestão assertiva de recursos financeiros exige controle transacional rigoroso para prevenir duplicidade de lançamentos e distorções de saldo. Este projeto implementa uma API RESTful voltada para controle de receitas, despesas e apuração de saldos consolidados com persistência relacional e integridade ACID.

## ⚙️ Arquitetura & Decisões Técnicas
- **Consistência Transacional:** Operações de débito/crédito controladas por anotações `@Transactional` no Spring, assegurando atomicidade contábil.
- **Isolamento de Domínio:** Separação estrita entre entidades de persistência, objetos de transferência de dados (DTOs) e regras de cálculo financeiro.
- **Modelagem Relacional Otimizada:** Estruturação no PostgreSQL com chaves estrangeiras, índices em campos de data/usuário e restrições de integridade.

## 🔌 Principais Operações da API
- `POST /api/transacoes`: Registro de nova despesa/receita com categoria e timestamp.
- `GET /api/transacoes/balanco`: Apuração de saldo consolidado em determinado período.
- `GET /api/transacoes/relatorio`: Extração de lançamentos agrupados por centro de custo.

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

