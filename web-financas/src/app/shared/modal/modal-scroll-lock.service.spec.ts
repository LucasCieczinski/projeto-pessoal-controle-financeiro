import { TestBed } from '@angular/core/testing';
import { ModalScrollLock } from './modal-scroll-lock.service';

describe('ModalScrollLock', () => {
  it('mantém o bloqueio enquanto houver outro modal e preserva estilos anteriores', () => {
    const lock = TestBed.inject(ModalScrollLock);
    const style = document.documentElement.style;
    const originalOverflow = style.overflow;
    const originalGutter = style.scrollbarGutter;
    const first = {};
    const second = {};
    try {
      style.overflow = 'auto';
      lock.lock(first);
      lock.lock(first);
      lock.lock(second);
      lock.unlock(first);
      expect(style.overflow).toBe('hidden');
      lock.unlock(second);
      expect(style.overflow).toBe('auto');
      lock.unlock(second);
      expect(style.overflow).toBe('auto');
    } finally {
      lock.unlock(first);
      lock.unlock(second);
      style.overflow = originalOverflow;
      style.scrollbarGutter = originalGutter;
    }
  });
});
