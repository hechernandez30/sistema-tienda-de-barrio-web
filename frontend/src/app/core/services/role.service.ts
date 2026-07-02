import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Role } from '../models/user.model';

export interface RoleCreateRequest {
  name: string;
  description?: string | null;
  active: boolean;
}

export interface RoleUpdateRequest {
  description?: string | null;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/roles`;

  list(): Observable<Role[]> {
    return this.http.get<Role[]>(this.baseUrl);
  }

  getById(id: string): Observable<Role> {
    return this.http.get<Role>(`${this.baseUrl}/${id}`);
  }

  // Métodos opcionales de administración de roles (los endpoints existen en el backend).
  create(payload: RoleCreateRequest): Observable<Role> {
    return this.http.post<Role>(this.baseUrl, payload);
  }

  update(id: string, payload: RoleUpdateRequest): Observable<Role> {
    return this.http.put<Role>(`${this.baseUrl}/${id}`, payload);
  }

  activate(id: string): Observable<Role> {
    return this.http.patch<Role>(`${this.baseUrl}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<Role> {
    return this.http.patch<Role>(`${this.baseUrl}/${id}/deactivate`, {});
  }
}
