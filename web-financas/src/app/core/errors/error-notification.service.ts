import { Injectable, signal } from '@angular/core';

export type ToastTone = 'info' | 'warning' | 'error';

export interface ToastMessage {
  id: number;
  message: string;
  tone: ToastTone;
}

const TOAST_DURATION_MS = 6000;
const MAX_VISIBLE_TOASTS = 3;

/**
 * Serviço centralizado de feedback de erros de API.
 *
 * Alimenta o `ToastHostComponent` (região `aria-live`) com mensagens educadas,
 * evitando que cada componente precise reimplementar o mapeamento de status
 * HTTP para texto amigável. O erro original continua sendo propagado pelo
 * interceptor para que os componentes tratem o fluxo (navegação, retry, etc).
 */
@Injectable({ providedIn: 'root' })
export class ErrorNotificationService {
  private readonly state = signal<ToastMessage[]>([]);
  private nextId = 1;

  /** Lista de toasts visíveis (no máximo {@link MAX_VISIBLE_TOASTS}). */
  readonly toasts = this.state.asReadonly();

  show(message: string, tone: ToastTone = 'error'): void {
    const toast: ToastMessage = { id: this.nextId++, message, tone };
    this.state.update((current) => [...current, toast].slice(-MAX_VISIBLE_TOASTS));
    setTimeout(() => this.dismiss(toast.id), TOAST_DURATION_MS);
  }

  dismiss(id: number): void {
    this.state.update((current) => current.filter((toast) => toast.id !== id));
  }
}
