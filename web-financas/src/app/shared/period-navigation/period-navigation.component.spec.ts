import { TestBed } from '@angular/core/testing';
import { PeriodNavigationComponent } from './period-navigation.component';

describe('PeriodNavigationComponent', () => {
  it('navega pelo período e impede avanço além do mês atual', async () => {
    await TestBed.configureTestingModule({ imports: [PeriodNavigationComponent] }).compileComponents();
    const fixture = TestBed.createComponent(PeriodNavigationComponent);
    fixture.componentRef.setInput('label', 'Setembro de 2026');
    fixture.componentRef.setInput('month', 9);
    fixture.componentRef.setInput('isCurrentMonth', true);
    fixture.detectChanges();
    const changes: number[] = [];
    let currentMonthRequests = 0;
    fixture.componentInstance.monthChanged.subscribe((value) => changes.push(value));
    fixture.componentInstance.currentMonthRequested.subscribe(() => currentMonthRequests++);
    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Setembro de 2026');
    expect(buttons[1].disabled).toBe(true);
    expect(buttons[2].disabled).toBe(true);
    buttons[0].click();
    expect(changes).toEqual([-1]);
    fixture.componentRef.setInput('isCurrentMonth', false);
    fixture.detectChanges();
    buttons[1].click();
    buttons[2].click();
    expect(currentMonthRequests).toBe(1);
    expect(changes).toEqual([-1, 1]);
  });
});
