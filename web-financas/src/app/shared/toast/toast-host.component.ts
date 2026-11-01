import { Component, inject } from '@angular/core';
import { ErrorNotificationService } from '../../core/errors/error-notification.service';

/**
 * Hospedeiro global dos toasts de erro (região aria-live).
 * Renderizado uma única vez na raiz do aplicativo (`app.ts`).
 */
@Component({
  selector: 'app-toast-host',
  templateUrl: './toast-host.component.html',
  styleUrl: './toast-host.component.scss',
})
export class ToastHostComponent {
  readonly notifications = inject(ErrorNotificationService);
}
