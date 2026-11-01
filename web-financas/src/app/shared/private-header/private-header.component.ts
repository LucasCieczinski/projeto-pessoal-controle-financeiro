import { Component, DOCUMENT, inject, input, OnDestroy, output, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { SidebarStateService } from './sidebar-state.service';

/**
 * Navegação principal da área autenticada.
 *
 * - Desktop (>= 901px): sidebar fixa colapsável com marca, links, atalho rápido
 *   de "Nova movimentação", links de preparação da Onda 2 e área da conta.
 * - Mobile (<= 620px): cabeçalho compacto (marca + mês ativo + avatar),
 *   barra inferior fixa (bottom navigation) e menu de ações da conta.
 */
@Component({
  selector: 'app-private-header',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './private-header.component.html',
  styleUrl: './private-header.component.scss',
})
export class PrivateHeaderComponent implements OnDestroy {
  /** Contexto exibido no cabeçalho compacto do mobile (ex.: mês ativo). */
  readonly context = input('');
  readonly logoutRequested = output<void>();
  readonly sidebar = inject(SidebarStateService);

  /** Controla o menu de ações da conta (avatar / "Mais") no mobile. */
  readonly accountMenuOpen = signal(false);

  private readonly document = inject(DOCUMENT);

  constructor() {
    // Reserva espaço no rodapé da página para a bottom navigation (CSS no body).
    this.document.body.classList.add('has-bottom-nav');
  }

  toggleAccountMenu(): void {
    this.accountMenuOpen.update((open) => !open);
  }

  closeAccountMenu(): void {
    this.accountMenuOpen.set(false);
  }

  requestLogout(): void {
    this.closeAccountMenu();
    this.logoutRequested.emit();
  }

  ngOnDestroy(): void {
    this.document.body.classList.remove('has-bottom-nav');
  }
}
