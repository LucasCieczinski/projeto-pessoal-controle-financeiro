import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ModalComponent, ModalCloseReason } from './modal.component';

@Component({
  imports: [ModalComponent],
  template: `<button class="opener" (click)="open.set(true)">Abrir</button>
    <app-modal [(open)]="open" title="Novo registro" description="Preencha os dados."
      submitForm="test-form" [busy]="busy()" [closeOnBackdrop]="backdrop()" (closed)="reasons.push($event)">
      <form id="test-form" (submit)="$event.preventDefault(); submissions = submissions + 1">
        <input modalInitialFocus aria-label="Descrição" />
      </form>
    </app-modal>`,
})
class ModalHost {
  open = signal(false);
  busy = signal(false);
  backdrop = signal(false);
  reasons: ModalCloseReason[] = [];
  submissions = 0;
}

async function setup() {
  await TestBed.configureTestingModule({ imports: [ModalHost] }).compileComponents();
  const fixture = TestBed.createComponent(ModalHost);
  fixture.detectChanges();
  const element = fixture.nativeElement as HTMLElement;
  const dialog = element.querySelector('dialog')!;
  // The DOM test environment does not reproduce native top-layer/focus trapping.
  // Stub only these browser methods; real keyboard behavior is checked in-browser.
  Object.defineProperty(dialog, 'showModal', { configurable: true, value: vi.fn(() => dialog.setAttribute('open', '')) });
  Object.defineProperty(dialog, 'close', { configurable: true, value: vi.fn(() => dialog.removeAttribute('open')) });
  const opener = element.querySelector<HTMLButtonElement>('.opener')!;
  opener.focus();
  opener.click();
  await fixture.whenStable();
  return { fixture, dialog, opener, host: fixture.componentInstance };
}

describe('ModalComponent', () => {
  it('associa título e descrição, foca o conteúdo e devolve o foco ao fechar', async () => {
    const previousOverflow = document.documentElement.style.overflow;
    const { fixture, dialog, opener, host } = await setup();
    expect(dialog.open).toBe(true);
    expect(dialog.querySelector('h2')?.id).toBe(dialog.getAttribute('aria-labelledby'));
    expect(dialog.querySelector('.heading p')?.id).toBe(dialog.getAttribute('aria-describedby'));
    expect(document.activeElement).toBe(dialog.querySelector('input'));
    expect(document.documentElement.style.overflow).toBe('hidden');
    dialog.querySelector<HTMLButtonElement>('.secondary-action')!.click();
    await fixture.whenStable();
    expect(dialog.open).toBe(false);
    expect(host.open()).toBe(false);
    expect(host.reasons).toEqual(['cancel']);
    expect(document.activeElement).toBe(opener);
    expect(document.documentElement.style.overflow).toBe(previousOverflow);
  });

  it('envia o formulário projetado sem fechar automaticamente', async () => {
    const { dialog, host } = await setup();
    dialog.querySelector<HTMLButtonElement>('.confirm-action')!.click();
    expect(host.submissions).toBe(1);
    expect(dialog.open).toBe(true);
  });

  it('bloqueia ações e Escape durante o envio, mas permite fechamento pelo dono', async () => {
    const { fixture, dialog, host } = await setup();
    host.busy.set(true);
    await fixture.whenStable();
    expect(dialog.querySelector<HTMLButtonElement>('.confirm-action')!.disabled).toBe(true);
    expect(dialog.querySelector<HTMLButtonElement>('.close-action')!.disabled).toBe(true);
    const cancel = new Event('cancel', { cancelable: true });
    dialog.dispatchEvent(cancel);
    expect(cancel.defaultPrevented).toBe(true);
    expect(host.open()).toBe(true);
    host.open.set(false);
    await fixture.whenStable();
    expect(dialog.open).toBe(false);
    expect(host.reasons).toEqual(['programmatic']);
  });

  it('fecha por Escape quando não está ocupado e emite somente uma vez', async () => {
    const { fixture, dialog, host } = await setup();
    dialog.dispatchEvent(new Event('cancel', { cancelable: true }));
    await fixture.whenStable();
    dialog.dispatchEvent(new Event('close'));
    expect(host.open()).toBe(false);
    expect(host.reasons).toEqual(['escape']);
  });

  it('protege contra clique externo por padrão e permite habilitá-lo', async () => {
    const { fixture, dialog, host } = await setup();
    const outsideClick = () => {
      dialog.dispatchEvent(new MouseEvent('pointerdown', { clientX: -10, clientY: -10 }));
      dialog.dispatchEvent(new MouseEvent('click', { clientX: -10, clientY: -10 }));
    };
    outsideClick();
    expect(host.open()).toBe(true);
    host.backdrop.set(true);
    await fixture.whenStable();
    outsideClick();
    await fixture.whenStable();
    expect(host.open()).toBe(false);
    expect(host.reasons).toEqual(['backdrop']);
  });

  it('restaura a rolagem ao destruir um modal aberto', async () => {
    const previousOverflow = document.documentElement.style.overflow;
    const { fixture } = await setup();
    fixture.destroy();
    expect(document.documentElement.style.overflow).toBe(previousOverflow);
  });
});
