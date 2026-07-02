import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  PurchaseCreateRequest,
  PurchaseDetail,
  PurchaseListItem,
} from '../models/purchase.model';

@Injectable({ providedIn: 'root' })
export class PurchaseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/purchases`;

  list(): Observable<PurchaseListItem[]> {
    return this.http.get<PurchaseListItem[]>(this.baseUrl);
  }

  getById(id: string): Observable<PurchaseDetail> {
    return this.http.get<PurchaseDetail>(`${this.baseUrl}/${id}`);
  }

  create(payload: PurchaseCreateRequest): Observable<PurchaseDetail> {
    return this.http.post<PurchaseDetail>(this.baseUrl, payload);
  }

  confirm(id: string): Observable<PurchaseDetail> {
    return this.http.post<PurchaseDetail>(`${this.baseUrl}/${id}/confirm`, {});
  }

  cancel(id: string): Observable<PurchaseDetail> {
    return this.http.post<PurchaseDetail>(`${this.baseUrl}/${id}/cancel`, {});
  }
}
