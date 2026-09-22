import { DOCUMENT } from '@angular/common';
import { Component, effect, ElementRef, inject, input, model, OnDestroy, output, viewChild } from '@angular/core';
import { ModalScrollLock } from './modal-scroll-lock.service';

export type ModalCloseReason = 'cancel' | 'escape' | 'backdrop' | 'close-button' | 'programmatic';
let nextModalId = 0;

@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.scss',
})
export class ModalComponent implements OnDestroy {
  readonly open = model(false);
  readonly title = input.required<string>();
  readonly description = input('');
  readonly variant = input<'center' | 'drawer'>('center');
  readonly size = input<'regular' | 'wide'>('regular');
  readonly tone = input<'default' | 'danger'>('default');
  readonly confirmLabel = input('Confirmar');
  readonly cancelLabel = input('Cancelar');
  readonly busyLabel = input('Aguarde...');
  readonly busy = input(false);
  readonly confirmDisabled = input(false);
  readonly dismissible = input(true);
  readonly closeOnBackdrop = input(false);
  readonly submitForm = input('');
  readonly confirmed = output<void>();
  readonly closed = output<ModalCloseReason>();
  readonly titleId = `atona-modal-${++nextModalId}-title`;
  readonly descriptionId = `${this.titleId}-description`;

  private readonly dialog = viewChild<ElementRef<HTMLDialogElement>>('dialog');
  private readonly document = inject(DOCUMENT);
  private readonly scrollLock = inject(ModalScrollLock);
  private opener: HTMLElement | null = null;
  private active = false;
  private backdropPress = false;

  constructor() {
    effect(() => {
      const dialog = this.dialog()?.nativeElement;
      if (!dialog) return;
      if (this.open() && !dialog.open) {
        this.opener = this.document.activeElement as HTMLElement | null;
        dialog.showModal();
        this.active = true;
        this.scrollLock.lock(this);
        dialog.querySelector<HTMLElement>('[modalInitialFocus]')?.focus();
      } else if (!this.open() && dialog.open) {
        dialog.close();
        this.finish('programmatic');
      }
    });
  }

  requestClose(reason: ModalCloseReason): void {
    if (this.busy() || !this.dismissible()) return;
    this.open.set(false);
    this.dialog()?.nativeElement.close();
    this.finish(reason);
  }

  onCancel(event: Event): void {
    event.preventDefault();
    this.requestClose('escape');
  }

  onNativeClose(): void {
    // A queued close event from a previous opening must not close a reopened modal.
    if (this.dialog()?.nativeElement.open) return;
    this.open.set(false);
    this.finish('programmatic');
  }

  onPointerDown(event: PointerEvent): void {
    this.backdropPress = this.isBackdrop(event);
  }

  onClick(event: MouseEvent): void {
    const dismiss = this.backdropPress && this.isBackdrop(event);
    this.backdropPress = false;
    if (dismiss && this.closeOnBackdrop()) this.requestClose('backdrop');
  }

  confirm(): void {
    if (!this.busy() && !this.confirmDisabled() && !this.submitForm()) this.confirmed.emit();
  }

  ngOnDestroy(): void {
    const dialog = this.dialog()?.nativeElement;
    if (dialog?.open) dialog.close();
    this.scrollLock.unlock(this);
    if (this.active && this.opener?.isConnected) this.opener.focus();
  }

  private isBackdrop(event: MouseEvent): boolean {
    const dialog = this.dialog()?.nativeElement;
    if (!dialog || event.target !== dialog) return false;
    const rect = dialog.getBoundingClientRect();
    return event.clientX < rect.left || event.clientX > rect.right
      || event.clientY < rect.top || event.clientY > rect.bottom;
  }

  private finish(reason: ModalCloseReason): void {
    if (!this.active) return;
    this.active = false;
    this.scrollLock.unlock(this);
    if (this.opener?.isConnected) this.opener.focus();
    this.closed.emit(reason);
  }
}
