import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CustomerDetail,
  CustomerListItem,
  CustomerRequest,
} from '../models/customer.model';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/customers`;

  list(): Observable<CustomerListItem[]> {
    return this.http.get<CustomerListItem[]>(this.baseUrl);
  }

  /** Búsqueda por nombre, NIT, teléfono o email. Reutilizable por el POS. */
  search(term: string): Observable<CustomerListItem[]> {
    const params = new HttpParams().set('term', term);
    return this.http.get<CustomerListItem[]>(`${this.baseUrl}/search`, { params });
  }

  getById(id: string): Observable<CustomerDetail> {
    return this.http.get<CustomerDetail>(`${this.baseUrl}/${id}`);
  }

  create(payload: CustomerRequest): Observable<CustomerDetail> {
    return this.http.post<CustomerDetail>(this.baseUrl, payload);
  }

  update(id: string, payload: CustomerRequest): Observable<CustomerDetail> {
    return this.http.put<CustomerDetail>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
