import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-slate-100 via-white to-brand-50 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950">
      <router-outlet></router-outlet>
    </div>
  `,
})
export class PublicLayoutComponent {}
