import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ErrorNotificationService } from './error-notification.service';

describe('ErrorNotificationService', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('mantém no máximo três avisos simultâneos, descartando os mais antigos', () => {
    const service = new ErrorNotificationService();
    service.show('um');
    service.show('dois');
    service.show('três');
    service.show('quatro');
    expect(service.toasts().map((toast) => toast.message)).toEqual(['dois', 'três', 'quatro']);
  });

  it('remove o aviso automaticamente após o tempo configurado', () => {
    const service = new ErrorNotificationService();
    service.show('aviso');
    expect(service.toasts()).toHaveLength(1);
    vi.advanceTimersByTime(6000);
    expect(service.toasts()).toHaveLength(0);
  });

  it('preserva a ordem de criação e o tom de cada aviso', () => {
    const service = new ErrorNotificationService();
    service.show('sessão', 'warning');
    service.show('servidor', 'error');
    expect(service.toasts().map((toast) => `${toast.tone}:${toast.message}`)).toEqual([
      'warning:sessão',
      'error:servidor',
    ]);
  });

  it('ignora o fechamento de um aviso que já saiu da lista', () => {
    const service = new ErrorNotificationService();
    service.show('primeiro');
    service.dismiss(999);
    expect(service.toasts()).toHaveLength(1);
  });
});
