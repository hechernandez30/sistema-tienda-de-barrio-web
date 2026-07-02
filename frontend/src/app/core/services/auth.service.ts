import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { AuthUser, LoginResponse } from '../models/auth.model';

const TOKEN_KEY = 'tdb_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly currentUserSig = signal<AuthUser | null>(null);

  /** Usuario autenticado actual como signal de solo lectura. */
  readonly currentUser = this.currentUserSig.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUserSig() !== null);

  login(username: string, password: string): Observable<AuthUser> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, { username, password })
      .pipe(
        tap((response) => this.setToken(response.accessToken)),
        switchMap(() => this.loadCurrentUser()),
      );
  }

  loadCurrentUser(): Observable<AuthUser> {
    return this.http
      .get<AuthUser>(`${environment.apiUrl}/auth/me`)
      .pipe(tap((user) => this.currentUserSig.set(user)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.currentUserSig.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getCurrentUser(): AuthUser | null {
    return this.currentUserSig();
  }

  hasRole(role: string): boolean {
    return this.currentUserSig()?.role === role;
  }

  hasAnyRole(roles: string[]): boolean {
    const role = this.currentUserSig()?.role;
    return role != null && roles.includes(role);
  }

  private setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
  }
}
