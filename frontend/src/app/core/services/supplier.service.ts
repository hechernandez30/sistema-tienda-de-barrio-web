import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  SupplierDetail,
  SupplierListItem,
  SupplierRequest,
} from '../models/supplier.model';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/suppliers`;

  list(): Observable<SupplierListItem[]> {
    return this.http.get<SupplierListItem[]>(this.baseUrl);
  }

  search(term: string): Observable<SupplierListItem[]> {
    const params = new HttpParams().set('term', term);
    return this.http.get<SupplierListItem[]>(`${this.baseUrl}/search`, { params });
  }

  getById(id: string): Observable<SupplierDetail> {
    return this.http.get<SupplierDetail>(`${this.baseUrl}/${id}`);
  }

  create(payload: SupplierRequest): Observable<SupplierDetail> {
    return this.http.post<SupplierDetail>(this.baseUrl, payload);
  }

  update(id: string, payload: SupplierRequest): Observable<SupplierDetail> {
    return this.http.put<SupplierDetail>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
