# Modal padrão À Tona

`ModalComponent` é standalone. Importar na tela consumidora e controlar a abertura
por um signal. O componente não acessa serviços financeiros nem conhece o conteúdo
do formulário. Movimentações é a primeira integração.

## Formulário

```ts
import { signal } from '@angular/core';
import { ModalComponent } from './shared/modal/modal.component';

// Adicionar ModalComponent aos imports da tela.
readonly editorOpen = signal(false);
isSaving = false;
```

```html
<button type="button" (click)="editorOpen.set(true)">Novo registro</button>
<app-modal [(open)]="editorOpen"
  title="Novo registro"
  description="Preencha as informações abaixo."
  confirmLabel="Salvar" busyLabel="Salvando..."
  [busy]="isSaving" submitForm="record-form">
  <form id="record-form" [formGroup]="form" (ngSubmit)="save()" novalidate>
    <fieldset [disabled]="isSaving">
      <label for="record-description">Descrição</label>
      <input id="record-description" modalInitialFocus formControlName="description" />
    </fieldset>
  </form>
</app-modal>
```

`submitForm` deve corresponder ao ID **único** do formulário projetado. O botão
principal envia esse formulário inclusive por teclado, sem duplicar o evento.
Não usar `method="dialog"`: a tela deve validar, salvar e então definir
`editorOpen.set(false)`. Em falha, manter aberto e apresentar o erro no conteúdo.
O consumidor continua responsável pelas validações, campos, bloqueio dos campos
durante o envio e prevenção de envios duplicados no método de salvar.

## Confirmação sem formulário

Omitir `submitForm` e tratar `(confirmed)="confirm()"`. O modal não fecha sozinho:
o consumidor determina quando a operação terminou e altera o signal `open`.
Usar `confirmLabel=""` para omitir a ação principal.

Para exclusão, usar `tone="danger"`, nomear a ação (por exemplo, "Excluir
movimentação") e mostrar qual registro será afetado. A opção de cancelar deve
permanecer disponível antes do envio. Movimentações reutiliza este padrão para
criação, edição e confirmação de exclusão, sem duplicar a estrutura do modal.

## Configuração e eventos

| Propriedade | Padrão | Uso |
| --- | --- | --- |
| `title` | obrigatório | Nome acessível e título visível |
| `description` | vazio | Explicação opcional ligada por ARIA |
| `variant` | `center` | `center` ou `drawer`, com o mesmo padrão visual |
| `size` | `regular` | `regular` ou `wide`; largura maior apenas no centralizado |
| `tone` | `default` | `danger` destaca a ação destrutiva, mantendo o mesmo layout |
| `confirmLabel` / `cancelLabel` | Confirmar / Cancelar | Textos; vazio oculta a respectiva ação |
| `busy` / `busyLabel` | false / Aguarde... | Desabilita ações e fechamento; anuncia processamento |
| `confirmDisabled` | false | Desabilita apenas a ação principal |
| `dismissible` | true | Permite X, Cancelar e Escape |
| `closeOnBackdrop` | false | Fechamento externo opcional; não descarta formulários por acidente |

`closed` informa `cancel`, `escape`, `backdrop`, `close-button` ou `programmatic`.
`openChange` é exposto pelo two-way binding `[(open)]`. Fechamento programático
continua possível mesmo durante `busy` ou com `dismissible=false`.

## UX e acessibilidade

- Cabeçalho e ações permanecem visíveis; somente o conteúdo rola.
- Usa `<dialog>.showModal()`: conteúdo externo inerte e contenção nativa de foco.
- `modalInitialFocus` marca o campo inicial; na ausência, o título recebe foco.
- Ao fechar, o foco retorna ao elemento que abriu, se ele ainda existir.
- Título e descrição têm identificadores únicos por instância.
- Escape respeita `busy` e `dismissible`. Clique fora é desativado por padrão.
- Bloqueio de rolagem compartilhado suporta múltiplos modais e restaura os
  estilos anteriores quando o último fecha ou é destruído.
- Respeita movimento reduzido; botão de fechar possui alvo de 44 × 44 px.
- Para uso não descartável, fornecer sempre uma ação viável de conclusão/saída.

As regras de tema, tamanho e ações estão em `modal.component.scss`. Não copiar
essa estrutura para novas telas nem sobrescrevê-la com `::ng-deep`. O conteúdo
projetado mantém os estilos e regras da própria funcionalidade.
