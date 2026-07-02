import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ProductCreatePayload,
  ProductDetail,
  ProductListItem,
  ProductPos,
  ProductUpdatePayload,
} from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/products`;

  list(): Observable<ProductListItem[]> {
    return this.http.get<ProductListItem[]>(this.baseUrl);
  }

  getById(id: string): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${this.baseUrl}/${id}`);
  }

  getByBarcode(barcode: string): Observable<ProductPos> {
    return this.http.get<ProductPos>(`${this.baseUrl}/barcode/${barcode}`);
  }

  search(term: string): Observable<ProductPos[]> {
    const params = new HttpParams().set('term', term);
    return this.http.get<ProductPos[]>(`${this.baseUrl}/search`, { params });
  }

  create(payload: ProductCreatePayload): Observable<ProductDetail> {
    return this.http.post<ProductDetail>(this.baseUrl, payload);
  }

  update(id: string, payload: ProductUpdatePayload): Observable<ProductDetail> {
    return this.http.put<ProductDetail>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
