# À Tona — aplicação web

Frontend Angular da marca À Tona, projeto de controle financeiro pessoal. A identidade visual parte da ideia de trazer o dinheiro à tona e olhar para ele com clareza, um mês de cada vez.

## Páginas

- `/`: página institucional pública, com proposta, princípios e estágio real do produto.
- `/login`: acesso à conta existente.
- `/cadastro`: criação de conta.
- `/inicio`: resumo do usuário autenticado.
- `/movimentacoes`: extrato do mês, com criação, edição e exclusão de movimentações.

Todas as telas consomem a API Spring Boot e nenhuma exibe dados financeiros fictícios.

## Navegação

- **Desktop (a partir de 901px):** barra lateral fixa e colapsável, com resumo,
  movimentações, atalho *Nova movimentação* e a área da conta. Rendas e Cartões
  aparecem desabilitados com o selo *Em breve* enquanto a Onda 2 não chega.
- **Mobile (até 620px):** cabeçalho compacto com o mês ativo e o avatar, barra
  inferior fixa (Início, Movimentações, atalho de adicionar e *Mais*) e menu de conta
  com a saída da sessão.

## Layout das páginas autenticadas

A barra lateral flutua sobre a página, então cada tela reserva o espaço dela e
centraliza a própria coluna de conteúdo. As medidas da barra ficam em
`src/styles.scss` como fonte única: `--sidebar-w` e `--sidebar-gap` (76px/24px
recolhida, 248px/32px expandida, sob `body.sidebar-expanded`). O componente usa a
largura e as páginas usam o deslocamento, então barra e conteúdo mudam juntos na
mesma animação de 180ms.

Página autenticada nova deve incluir o mixin que aplica esse recuo no host e o teto
de 1440px centralizado no conteúdo:

```scss
@use '../../shared/page-shell' as shell;

@include shell.authenticated-page();
```

O botão de recolher/expandir fica no cabeçalho da barra, na linha da marca (padrão
de cabeçalho de sidebar), com o ícone de painel e a seta apontando para o lado da
ação, tooltip e `aria-expanded`. Ele existe apenas no desktop.

## Tratamento de erros

O `apiErrorInterceptor` centraliza o feedback de falhas da API: sessão expirada
(`401`), falta de permissão (`403`) e falhas do servidor (`5xx`) geram avisos no
`app-toast-host`, uma região `aria-live`. Erros de validação e de recurso ausente
(`400`/`404`) continuam sendo exibidos junto ao campo ou ao bloco correspondente,
sem mensagem duplicada.

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
