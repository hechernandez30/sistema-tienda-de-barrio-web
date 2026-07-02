import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  SaleCreateRequest,
  SaleDetail,
  SaleListItem,
} from '../models/sale.model';

@Injectable({ providedIn: 'root' })
export class SalesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/sales`;

  list(): Observable<SaleListItem[]> {
    return this.http.get<SaleListItem[]>(this.baseUrl);
  }

  today(): Observable<SaleListItem[]> {
    return this.http.get<SaleListItem[]>(`${this.baseUrl}/today`);
  }

  byDate(date: string): Observable<SaleListItem[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<SaleListItem[]>(`${this.baseUrl}/by-date`, { params });
  }

  getById(id: string): Observable<SaleDetail> {
    return this.http.get<SaleDetail>(`${this.baseUrl}/${id}`);
  }

  create(payload: SaleCreateRequest): Observable<SaleDetail> {
    return this.http.post<SaleDetail>(this.baseUrl, payload);
  }

  cancel(id: string): Observable<SaleDetail> {
    return this.http.post<SaleDetail>(`${this.baseUrl}/${id}/cancel`, {});
  }
}
