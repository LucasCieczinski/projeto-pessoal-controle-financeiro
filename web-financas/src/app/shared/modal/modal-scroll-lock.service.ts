import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ModalScrollLock {
  private readonly document = inject(DOCUMENT);
  private readonly owners = new Set<object>();
  private overflow = '';
  private gutter = '';

  lock(owner: object): void {
    if (this.owners.has(owner)) return;
    const style = this.document.documentElement.style;
    if (!this.owners.size) {
      this.overflow = style.overflow;
      this.gutter = style.scrollbarGutter;
      style.scrollbarGutter = 'stable';
      style.overflow = 'hidden';
    }
    this.owners.add(owner);
  }

  unlock(owner: object): void {
    if (!this.owners.delete(owner) || this.owners.size) return;
    const style = this.document.documentElement.style;
    style.overflow = this.overflow;
    style.scrollbarGutter = this.gutter;
  }
}
