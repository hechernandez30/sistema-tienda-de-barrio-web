import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { Observable, catchError, map, of } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Protege una ruta exigiendo uno de los roles indicados.
 * Si el usuario aún no está cargado en memoria (por ejemplo tras recargar la página),
 * lo carga desde el backend antes de decidir. Si no cumple el rol, redirige al dashboard.
 */
export function roleGuard(allowedRoles: string[]): CanActivateFn {
  return (): boolean | UrlTree | Observable<boolean | UrlTree> => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      return router.createUrlTree(['/login']);
    }

    const decide = (role: string | undefined): boolean | UrlTree =>
      role && allowedRoles.includes(role) ? true : router.createUrlTree(['/app/dashboard']);

    const current = authService.getCurrentUser();
    if (current) {
      return decide(current.role);
    }

    return authService.loadCurrentUser().pipe(
      map((user) => decide(user.role)),
      catchError(() => of(router.createUrlTree(['/login']))),
    );
  };
}
