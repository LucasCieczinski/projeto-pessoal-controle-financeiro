import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { CreateMovementRequest, Movement } from '../../core/movements/movement.models';
import { AuthService } from '../../core/auth/auth.service';
import { MovementService } from '../../core/movements/movement.service';
import { MovementsComponent, parseAmount } from './movements.component';

describe('MovementsComponent', () => {
  const record: Movement = { id: 'edit-1', descricao: 'Mercado', valor: 58.9, tipo: 'SAIDA', categoria: 'Alimentação', dataMovimentacao: '2025-01-10', dataCadastro: '' };

  function stubDialogs(page: HTMLElement): void {
    page.querySelectorAll('dialog').forEach((dialog) => {
      Object.defineProperty(dialog, 'showModal', { value: () => dialog.setAttribute('open', '') });
      Object.defineProperty(dialog, 'close', { value: () => dialog.removeAttribute('open') });
    });
  }

  it('preenche a edição, preserva o formulário quando falha e acompanha a mudança de mês', async () => {
    const response = new Subject<Movement>();
    const update = vi.fn().mockReturnValueOnce(response).mockReturnValue(of({ ...record, dataMovimentacao: '2025-02-10' }));
    const forMonth = vi.fn(() => of([record]));
    await TestBed.configureTestingModule({
      imports: [MovementsComponent],
      providers: [provideRouter([]), { provide: MovementService, useValue: { forMonth, update } }, { provide: AuthService, useValue: {} }],
    }).compileComponents();
    const fixture = TestBed.createComponent(MovementsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    stubDialogs(page);
    page.querySelector<HTMLButtonElement>('[aria-label="Editar Mercado"]')!.click();
    await fixture.whenStable();
    expect(fixture.componentInstance.form.controls.valor.value).toBe('58,90');
    expect(page.querySelector('dialog h2')?.textContent).toBe('Editar movimentação');
    fixture.componentInstance.form.controls.dataMovimentacao.setValue('2025-02-10');
    fixture.componentInstance.save();
    response.error(new HttpErrorResponse({ status: 500 }));
    await fixture.whenStable();
    expect(fixture.componentInstance.editorOpen()).toBe(true);
    expect(page.querySelector('.save-error')?.textContent).toContain('Não foi possível salvar');
    expect(fixture.componentInstance.form.controls.dataMovimentacao.value).toBe('2025-02-10');
    page.querySelector<HTMLButtonElement>('.confirm-action')!.click();
    await fixture.whenStable();
    expect(update).toHaveBeenLastCalledWith('edit-1', { descricao: 'Mercado', valor: 58.9, tipo: 'SAIDA', categoria: 'Alimentação', dataMovimentacao: '2025-02-10' });
    expect(fixture.componentInstance.editorOpen()).toBe(false);
    expect(forMonth).toHaveBeenLastCalledWith('2025-02');
    expect(page.textContent).toContain('Movimentação atualizada');
  });

  it('exige confirmação para excluir, permite cancelar e impede exclusão duplicada', async () => {
    const response = new Subject<void>();
    const remove = vi.fn(() => response);
    const forMonth = vi.fn().mockReturnValueOnce(of([record])).mockReturnValue(of([]));
    await TestBed.configureTestingModule({
      imports: [MovementsComponent],
      providers: [provideRouter([]), { provide: MovementService, useValue: { forMonth, delete: remove } }, { provide: AuthService, useValue: {} }],
    }).compileComponents();
    const fixture = TestBed.createComponent(MovementsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    stubDialogs(page);
    const opener = page.querySelector<HTMLButtonElement>('[aria-label="Excluir Mercado"]')!;
    opener.click();
    await fixture.whenStable();
    expect(remove).not.toHaveBeenCalled();
    const dialog = page.querySelectorAll('dialog')[1];
    expect(dialog.textContent).toContain('Mercado');
    dialog.querySelector<HTMLButtonElement>('.secondary-action')!.click();
    await fixture.whenStable();
    expect(remove).not.toHaveBeenCalled();
    expect(fixture.componentInstance.deleteOpen()).toBe(false);
    opener.click();
    await fixture.whenStable();
    dialog.querySelector<HTMLButtonElement>('.confirm-action')!.click();
    await fixture.whenStable();
    expect(remove).toHaveBeenCalledWith('edit-1');
    expect(dialog.querySelector<HTMLButtonElement>('.confirm-action')!.disabled).toBe(true);
    fixture.componentInstance.deleteMovement();
    expect(remove).toHaveBeenCalledTimes(1);
    response.next();
    response.complete();
    await fixture.whenStable();
    expect(fixture.componentInstance.deleteOpen()).toBe(false);
    expect(page.textContent).toContain('Movimentação excluída');
    expect(page.querySelectorAll('.movement-row')).toHaveLength(0);
  });

  it('envia pelo modal padrão, bloqueia envio duplicado e fecha após salvar', async () => {
    const response = new Subject<Movement>();
    const create = vi.fn((_request: CreateMovementRequest) => response);
    await TestBed.configureTestingModule({
      imports: [MovementsComponent],
      providers: [
        provideRouter([]),
        { provide: MovementService, useValue: { forMonth: () => of([]), create } },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(MovementsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    const dialog = page.querySelector('dialog')!;
    Object.defineProperty(dialog, 'showModal', { value: () => dialog.setAttribute('open', '') });
    Object.defineProperty(dialog, 'close', { value: () => dialog.removeAttribute('open') });
    page.querySelector<HTMLButtonElement>('.primary-action')!.click();
    await fixture.whenStable();
    const submit = page.querySelector<HTMLButtonElement>('.confirm-action')!;
    submit.click();
    await fixture.whenStable();
    expect(create).not.toHaveBeenCalled();
    expect(document.activeElement?.id).toBe('movement-description');
    fixture.componentInstance.form.setValue({
      tipo: 'SAIDA', descricao: ' Mercado ', valor: '58,90', categoria: 'Alimentação', dataMovimentacao: '2025-02-28',
    });
    submit.click();
    await fixture.whenStable();
    expect(create).toHaveBeenCalledWith({
      tipo: 'SAIDA', descricao: 'Mercado', valor: 58.9, categoria: 'Alimentação', dataMovimentacao: '2025-02-28',
    });
    expect(submit.disabled).toBe(true);
    expect(page.querySelector<HTMLFieldSetElement>('.editor-fields')!.disabled).toBe(true);
    dialog.dispatchEvent(new Event('cancel', { cancelable: true }));
    fixture.componentInstance.save();
    expect(create).toHaveBeenCalledTimes(1);
    expect(dialog.open).toBe(true);
    response.next({ ...create.mock.calls[0][0], id: 'test', dataCadastro: '2025-02-28T12:00:00' });
    response.complete();
    await fixture.whenStable();
    expect(dialog.open).toBe(false);
    expect(page.textContent).toContain('Movimentação salva com sucesso.');
  });

  it('atualiza a tela quando a consulta assíncrona termina e rejeita datas futuras', async () => {
    const response = new Subject<Movement[]>();
    await TestBed.configureTestingModule({
      imports: [MovementsComponent],
      providers: [
        provideRouter([]),
        { provide: MovementService, useValue: { forMonth: () => response } },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(MovementsComponent);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Buscando seus registros');
    response.next([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Nenhuma movimentação neste mês');
    const date = fixture.componentInstance.form.controls.dataMovimentacao;
    date.setValue('9999-12-31');
    expect(date.invalid).toBe(true);
    date.setValue('2025-02-30');
    expect(date.invalid).toBe(true);
    date.setValue('2025-02-28');
    expect(date.valid).toBe(true);
  });

  it('mostra o estado vazio sem valores inventados', async () => {
    await TestBed.configureTestingModule({
      imports: [MovementsComponent],
      providers: [
        provideRouter([]),
        { provide: MovementService, useValue: { forMonth: () => of([]) } },
        { provide: AuthService, useValue: { logout: () => of(void 0) } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(MovementsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;

    expect(page.querySelector('h1')?.textContent).toContain('Movimentações');
    expect(page.textContent).toContain('Nenhuma movimentação neste mês');
    expect(page.textContent).not.toContain('R$ 0,00');
    expect(page.querySelector('button.primary-action')).not.toBeNull();
  });

  it('filtra registros reais por tipo', async () => {
    await TestBed.configureTestingModule({
      imports: [MovementsComponent],
      providers: [
        provideRouter([]),
        {
          provide: MovementService,
          useValue: {
            forMonth: () => of([
              { id: '1', descricao: 'Salário', valor: 3200, tipo: 'ENTRADA', categoria: 'Trabalho', dataMovimentacao: '2026-09-10', dataCadastro: '2026-09-10T12:00:00' },
              { id: '2', descricao: 'Mercado', valor: 85, tipo: 'SAIDA', categoria: 'Alimentação', dataMovimentacao: '2026-09-10', dataCadastro: '2026-09-10T11:00:00' },
            ]),
          },
        },
        { provide: AuthService, useValue: { logout: () => of(void 0) } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(MovementsComponent);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    expect(page.textContent).toContain('Salário');
    expect(page.textContent).toContain('Mercado');
    expect(fixture.componentInstance.totalFor('ENTRADA')).toBe(3200);
    expect(fixture.componentInstance.totalFor('SAIDA')).toBe(85);

    const entryButton = Array.from(page.querySelectorAll('.type-filters button'))
      .find((button) => button.textContent?.includes('Entradas')) as HTMLButtonElement;
    entryButton.click();
    fixture.detectChanges();
    expect(page.textContent).toContain('Salário');
    expect(page.textContent).not.toContain('Mercado');
  });
});

describe('parseAmount', () => {
  it('aceita formato brasileiro sem perder os centavos', () => {
    expect(parseAmount('1.234,56')).toBe(1234.56);
    expect(parseAmount('58,90')).toBe(58.9);
    expect(parseAmount('0,00')).toBeNull();
    expect(parseAmount('1,234')).toBeNull();
  });

  it('mantém a quantia legível no formulário após o preenchimento', async () => {
    await TestBed.configureTestingModule({
      imports: [MovementsComponent],
      providers: [
        provideRouter([]),
        { provide: MovementService, useValue: { forMonth: () => of([]) } },
        { provide: AuthService, useValue: {} },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(MovementsComponent);
    fixture.componentInstance.form.controls.valor.setValue('1234,5');
    fixture.componentInstance.formatAmountField();
    expect(fixture.componentInstance.form.controls.valor.value).toBe('1.234,50');
  });
});
