import { Component, computed, inject } from '@angular/core';

import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  template: `
    <button
      type="button"
      (click)="toggle()"
      title="Cambiar tema"
      [attr.aria-label]="isDark() ? 'Activar modo claro' : 'Activar modo oscuro'"
      class="flex h-9 w-9 items-center justify-center rounded-xl text-slate-500 transition hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
    >
      <span class="material-icons !text-[20px]">{{ isDark() ? 'light_mode' : 'dark_mode' }}</span>
    </button>
  `,
})
export class ThemeToggleComponent {
  private readonly themeService = inject(ThemeService);

  readonly isDark = computed(() => this.themeService.theme() === 'dark');

  toggle(): void {
    this.themeService.toggleTheme();
  }
}
