import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  InventoryAdjustmentRequest,
  InventoryMovement,
  LowStockProduct,
  ProductLot,
} from '../models/inventory.model';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory`;

  listMovements(page = 0, size = 200): Observable<InventoryMovement[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<InventoryMovement[]>(`${this.baseUrl}/movements`, { params });
  }

  listProductMovements(productId: string): Observable<InventoryMovement[]> {
    return this.http.get<InventoryMovement[]>(`${this.baseUrl}/products/${productId}/movements`);
  }

  listProductLots(productId: string): Observable<ProductLot[]> {
    return this.http.get<ProductLot[]>(`${this.baseUrl}/products/${productId}/lots`);
  }

  expiringLots(days = 30): Observable<ProductLot[]> {
    const params = new HttpParams().set('days', days);
    return this.http.get<ProductLot[]>(`${this.baseUrl}/lots/expiring`, { params });
  }

  expiredLots(): Observable<ProductLot[]> {
    return this.http.get<ProductLot[]>(`${this.baseUrl}/lots/expired`);
  }

  lowStock(): Observable<LowStockProduct[]> {
    return this.http.get<LowStockProduct[]>(`${this.baseUrl}/low-stock`);
  }

  adjustIn(request: InventoryAdjustmentRequest): Observable<InventoryMovement> {
    return this.http.post<InventoryMovement>(`${this.baseUrl}/adjustments/in`, request);
  }

  adjustOut(request: InventoryAdjustmentRequest): Observable<InventoryMovement> {
    return this.http.post<InventoryMovement>(`${this.baseUrl}/adjustments/out`, request);
  }
}
