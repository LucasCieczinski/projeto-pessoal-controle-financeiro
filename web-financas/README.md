# À Tona — aplicação web

Frontend Angular da marca À Tona, projeto de controle financeiro pessoal. A identidade visual parte da ideia de trazer o dinheiro à tona e olhar para ele com clareza, um mês de cada vez.

## Páginas

- `/`: página institucional pública, com proposta, princípios e estágio real do produto.
- `/login`: acesso à conta existente.
- `/cadastro`: criação de conta.
- `/inicio`: área autenticada, atualmente uma recepção pessoal e próximos recursos.

Os formulários de login e cadastro consomem a API Spring Boot. As funcionalidades de movimentações e resumo mensal ainda estão planejadas; a interface não exibe dados financeiros fictícios.

A autenticação usa cookies HttpOnly para o JWT de acesso e para o refresh token.
O navegador renova a sessão automaticamente após a expiração do JWT; nenhum token
é guardado em `localStorage` ou `sessionStorage`. O interceptor envia credenciais
apenas à API e inclui a proteção CSRF nas operações que alteram dados. Após essa
mudança, é necessário entrar novamente na conta.

## Executar localmente

```bash
npm install
npm start
```

Abra `http://localhost:4200/`. Para testar cadastro e login, inicie a API e configure o endereço em `src/environments/environment.development.ts` conforme o ambiente local.

## Verificações

```bash
npm run build
npm test -- --watch=false
```
