import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PrivateHeaderComponent } from './private-header.component';

describe('PrivateHeaderComponent', () => {
  async function createFixture() {
    await TestBed.configureTestingModule({
      imports: [PrivateHeaderComponent],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(PrivateHeaderComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('inicia recolhido e alterna a navegação pelo botão acessível', async () => {
    const fixture = await createFixture();
    const header = fixture.nativeElement.querySelector('header') as HTMLElement;
    const button = fixture.nativeElement.querySelector('.menu-toggle') as HTMLButtonElement;
    expect(header.classList.contains('sidebar--expanded')).toBe(false);
    expect(button.getAttribute('aria-expanded')).toBe('false');
    expect(document.body.classList.contains('sidebar-expanded')).toBe(false);
    button.click();
    fixture.detectChanges();
    expect(header.classList.contains('sidebar--expanded')).toBe(true);
    expect(button.getAttribute('aria-expanded')).toBe('true');
    // O deslocamento do conteúdo das páginas é dirigido por esta classe do body.
    expect(document.body.classList.contains('sidebar-expanded')).toBe(true);
  });

  it('descreve a ação do botão de recolher/expandir em rótulo e dica', async () => {
    const fixture = await createFixture();
    const button = fixture.nativeElement.querySelector('.menu-toggle') as HTMLButtonElement;
    expect(button.getAttribute('aria-label')).toBe('Expandir navegação');
    expect(button.getAttribute('title')).toBe('Expandir navegação');

    button.click();
    fixture.detectChanges();
    expect(button.getAttribute('aria-label')).toBe('Recolher navegação');
    expect(button.getAttribute('title')).toBe('Recolher navegação');
  });

  it('expõe atalho rápido e links da Onda 2 desabilitados na sidebar', async () => {
    const fixture = await createFixture();
    const page = fixture.nativeElement as HTMLElement;

    const quickAction = page.querySelector<HTMLAnchorElement>('.quick-action');
    expect(quickAction?.getAttribute('href')).toBe('/movimentacoes?novo=1');
    expect(quickAction?.textContent).toContain('Nova movimentação');

    const upcoming = page.querySelectorAll<HTMLElement>('.upcoming-item');
    expect(upcoming).toHaveLength(2);
    const labels = Array.from(upcoming).map((item) => item.querySelector('span')?.textContent);
    expect(labels).toEqual(['Dívidas', 'Cartões']);
    for (const item of upcoming) {
      expect(item.getAttribute('aria-disabled')).toBe('true');
      expect(item.textContent).toContain('Em breve');
      expect(item.closest('a')).toBeNull();
    }
  });

  it('oferece barra inferior com os destinos essenciais e ação de adicionar', async () => {
    const fixture = await createFixture();
    const bottomNav = (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>('.bottom-nav');
    expect(bottomNav?.getAttribute('aria-label')).toBe('Navegação rápida');

    const links = Array.from(bottomNav?.querySelectorAll('a') ?? []);
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      '/inicio',
      '/movimentacoes',
      '/movimentacoes?novo=1',
      '/planejamento',
    ]);

    const add = bottomNav?.querySelector<HTMLAnchorElement>('.bottom-nav__add');
    expect(add?.getAttribute('aria-label')).toBe('Nova movimentação');
  });

  it('abre e fecha as ações da conta pelo avatar e emite logout', async () => {
    const fixture = await createFixture();
    const page = fixture.nativeElement as HTMLElement;
    const avatar = page.querySelector<HTMLButtonElement>('.mobile-avatar')!;
    let logoutCount = 0;
    fixture.componentInstance.logoutRequested.subscribe(() => logoutCount++);

    expect(avatar.getAttribute('aria-expanded')).toBe('false');
    expect(page.querySelector('.mobile-account')).toBeNull();
    avatar.click();
    fixture.detectChanges();
    expect(avatar.getAttribute('aria-expanded')).toBe('true');
    const menu = page.querySelector('.mobile-account');
    expect(menu).toBeTruthy();

    menu!.querySelector<HTMLButtonElement>('.logout')!.click();
    fixture.detectChanges();
    expect(logoutCount).toBe(1);
    expect(page.querySelector('.mobile-account')).toBeNull();
  });

  it('exibe o mês ativo no cabeçalho compacto quando informado', async () => {
    const fixture = await createFixture();
    expect((fixture.nativeElement as HTMLElement).querySelector('.header-context')).toBeNull();
    fixture.componentRef.setInput('context', 'Outubro de 2026');
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.header-context')?.textContent).toContain('Outubro de 2026');
  });
});
