import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

/**
 * Componente reutilizable para features aún no implementadas.
 * El título se recibe por la propiedad `data.title` de la ruta.
 */
@Component({
  selector: 'app-feature-placeholder',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section>
      <!-- Encabezado del módulo -->
      <div class="mb-6 flex items-center gap-3">
        <span class="material-icons !text-[22px] text-slate-400">chevron_right</span>
        <h1 class="text-xl font-semibold text-slate-800">{{ title }}</h1>
      </div>

      <!-- Estado: en construcción -->
      <div class="flex min-h-[55vh] items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white/60 p-8">
        <div class="flex max-w-sm flex-col items-center text-center">
          <div class="mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-100 text-blue-600">
            <span class="material-icons !text-[34px]">construction</span>
          </div>
          <h2 class="text-lg font-semibold text-slate-800">Módulo en preparación</h2>
          <p class="mt-2 text-sm text-slate-500">
            La sección <span class="font-medium text-slate-700">{{ title }}</span> está preparada
            y se implementará en una próxima fase del proyecto.
          </p>
          <a
            routerLink="/app/dashboard"
            class="mt-6 inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            <span class="material-icons !text-[20px]">arrow_back</span>
            Volver al dashboard
          </a>
        </div>
      </div>
    </section>
  `,
})
export class FeaturePlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  readonly title = (this.route.snapshot.data['title'] as string) ?? 'Módulo';
}
