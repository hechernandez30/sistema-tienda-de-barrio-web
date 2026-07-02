import { Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const THEME_KEY = 'theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly themeSig = signal<Theme>('light');

  /** Tema actual como signal de solo lectura (útil para íconos reactivos). */
  readonly theme = this.themeSig.asReadonly();

  /** Lee la preferencia guardada y la aplica. Llamar una vez al iniciar la app. */
  initializeTheme(): void {
    const stored = localStorage.getItem(THEME_KEY);
    const theme: Theme = stored === 'dark' ? 'dark' : 'light';
    this.applyTheme(theme);
  }

  setTheme(theme: Theme): void {
    this.applyTheme(theme);
    localStorage.setItem(THEME_KEY, theme);
  }

  toggleTheme(): void {
    this.setTheme(this.themeSig() === 'dark' ? 'light' : 'dark');
  }

  getCurrentTheme(): Theme {
    return this.themeSig();
  }

  isDarkMode(): boolean {
    return this.themeSig() === 'dark';
  }

  private applyTheme(theme: Theme): void {
    this.themeSig.set(theme);
    const root = document.documentElement;
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }
}
