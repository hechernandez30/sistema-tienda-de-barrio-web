import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { ThemeToggleComponent } from '../../shared/components/theme-toggle/theme-toggle.component';
import { NAV_ITEMS, NavItem } from '../nav-items';

@Component({
  selector: 'app-private-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ThemeToggleComponent],
  templateUrl: './private-layout.component.html',
})
export class PrivateLayoutComponent implements OnInit {
  private readonly authService = inject(AuthService);

  readonly user = this.authService.currentUser;
  readonly sidebarOpen = signal(false);

  readonly fullName = computed(() => {
    const current = this.user();
    return current ? `${current.firstName} ${current.lastName}`.trim() : '';
  });

  readonly initials = computed(() => {
    const current = this.user();
    if (!current) {
      return '';
    }
    const first = current.firstName?.charAt(0) ?? '';
    const last = current.lastName?.charAt(0) ?? '';
    return (first + last).toUpperCase();
  });

  readonly navItems = computed<NavItem[]>(() => {
    const role = this.user()?.role;
    if (!role) {
      return [];
    }
    return NAV_ITEMS.filter((item) => item.roles.includes(role));
  });

  ngOnInit(): void {
    // Al recargar la página el token persiste pero el usuario en memoria no; lo recuperamos.
    if (this.authService.isAuthenticated() && !this.authService.getCurrentUser()) {
      this.authService.loadCurrentUser().subscribe({ error: () => this.authService.logout() });
    }
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  logout(): void {
    this.authService.logout();
  }
}
