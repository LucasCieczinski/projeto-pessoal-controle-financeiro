import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-period-navigation',
  templateUrl: './period-navigation.component.html',
  styleUrl: './period-navigation.component.scss',
})
export class PeriodNavigationComponent {
  readonly variant = input<'rail' | 'toolbar'>('rail');
  readonly label = input.required<string>();
  readonly month = input.required<number>();
  readonly isCurrentMonth = input.required<boolean>();
  readonly monthChanged = output<number>();
  readonly currentMonthRequested = output<void>();
}
