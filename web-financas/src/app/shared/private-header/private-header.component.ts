import { Component, inject, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { SidebarStateService } from './sidebar-state.service';

@Component({
  selector: 'app-private-header',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './private-header.component.html',
  styleUrl: './private-header.component.scss',
})
export class PrivateHeaderComponent {
  readonly logoutRequested = output<void>();
  readonly sidebar = inject(SidebarStateService);
}
