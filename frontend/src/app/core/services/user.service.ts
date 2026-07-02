import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ChangePasswordRequest,
  UserCreateRequest,
  UserDetail,
  UserListItem,
  UserUpdateRequest,
} from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/users`;

  list(): Observable<UserListItem[]> {
    return this.http.get<UserListItem[]>(this.baseUrl);
  }

  getById(id: string): Observable<UserDetail> {
    return this.http.get<UserDetail>(`${this.baseUrl}/${id}`);
  }

  create(payload: UserCreateRequest): Observable<UserDetail> {
    return this.http.post<UserDetail>(this.baseUrl, payload);
  }

  update(id: string, payload: UserUpdateRequest): Observable<UserDetail> {
    return this.http.put<UserDetail>(`${this.baseUrl}/${id}`, payload);
  }

  activate(id: string): Observable<UserDetail> {
    return this.http.patch<UserDetail>(`${this.baseUrl}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<UserDetail> {
    return this.http.patch<UserDetail>(`${this.baseUrl}/${id}/deactivate`, {});
  }

  changePassword(id: string, payload: ChangePasswordRequest): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/password`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
