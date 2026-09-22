import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PrivateHeaderComponent } from './private-header.component';

describe('PrivateHeaderComponent', () => {
  it('inicia recolhido e alterna a navegação pelo botão acessível', async () => {
    await TestBed.configureTestingModule({
      imports: [PrivateHeaderComponent],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(PrivateHeaderComponent);
    fixture.detectChanges();
    const header = fixture.nativeElement.querySelector('header') as HTMLElement;
    const button = fixture.nativeElement.querySelector('.menu-toggle') as HTMLButtonElement;
    expect(header.classList.contains('sidebar--expanded')).toBe(false);
    expect(button.getAttribute('aria-expanded')).toBe('false');
    button.click();
    fixture.detectChanges();
    expect(header.classList.contains('sidebar--expanded')).toBe(true);
    expect(button.getAttribute('aria-expanded')).toBe('true');
  });
});
