import { DOCUMENT } from '@angular/common';
import { effect, inject, Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SidebarStateService {
  readonly expanded = signal(false);
  private readonly document = inject(DOCUMENT);

  constructor() {
    effect(() => this.document.body.classList.toggle('sidebar-expanded', this.expanded()));
  }

  toggle(): void {
    this.expanded.update((expanded) => !expanded);
  }
}
