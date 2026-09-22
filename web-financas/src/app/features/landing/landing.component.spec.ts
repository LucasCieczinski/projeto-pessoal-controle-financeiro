import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LandingComponent } from './landing.component';

describe('LandingComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('apresenta a marca e caminhos claros para cadastro e login', () => {
    const fixture = TestBed.createComponent(LandingComponent);
    fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('h1')?.textContent).toContain('Dinheiro é');
    expect(page.querySelector('.site-header .brand')?.getAttribute('aria-label')).toBe('À Tona, página inicial');
    expect(page.querySelector('.site-header .brand')?.textContent).toContain('à tona');
    expect(page.querySelector('a[routerLink="/cadastro"]')).toBeTruthy();
    expect(page.querySelector('a[routerLink="/login"]')).toBeTruthy();
  });

  it('distingue o que já existe do que ainda será construído', () => {
    const fixture = TestBed.createComponent(LandingComponent);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('DISPONÍVEL');
    expect(text).toContain('EM BREVE');
    expect(text).toContain('PLANEJADO');
  });
});
