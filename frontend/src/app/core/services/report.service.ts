import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CashByCategoryReport,
  CashSummaryReport,
  DailySalesReport,
  EstimatedProfitReport,
  InventorySummaryReport,
  LowStockReport,
  PurchasesBySupplierReport,
  PurchasesSummaryReport,
  SalesByCategoryReport,
  SalesByPaymentMethodReport,
  SalesSummaryReport,
  TopProductReport,
} from '../models/report.model';

export interface DateRange {
  from: string;
  to: string;
}

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reports`;

  private rangeParams(range: DateRange): HttpParams {
    return new HttpParams().set('from', range.from).set('to', range.to);
  }

  salesSummary(range: DateRange): Observable<SalesSummaryReport> {
    return this.http.get<SalesSummaryReport>(`${this.baseUrl}/sales-summary`, {
      params: this.rangeParams(range),
    });
  }

  salesByPaymentMethod(range: DateRange): Observable<SalesByPaymentMethodReport[]> {
    return this.http.get<SalesByPaymentMethodReport[]>(`${this.baseUrl}/sales-by-payment-method`, {
      params: this.rangeParams(range),
    });
  }

  dailySales(range: DateRange): Observable<DailySalesReport[]> {
    return this.http.get<DailySalesReport[]>(`${this.baseUrl}/daily-sales`, {
      params: this.rangeParams(range),
    });
  }

  salesByCategory(range: DateRange): Observable<SalesByCategoryReport[]> {
    return this.http.get<SalesByCategoryReport[]>(`${this.baseUrl}/sales-by-category`, {
      params: this.rangeParams(range),
    });
  }

  topProducts(range: DateRange, limit = 10): Observable<TopProductReport[]> {
    const params = this.rangeParams(range).set('limit', String(limit));
    return this.http.get<TopProductReport[]>(`${this.baseUrl}/top-products`, { params });
  }

  lowStock(): Observable<LowStockReport[]> {
    return this.http.get<LowStockReport[]>(`${this.baseUrl}/low-stock`);
  }

  inventorySummary(): Observable<InventorySummaryReport> {
    return this.http.get<InventorySummaryReport>(`${this.baseUrl}/inventory-summary`);
  }

  purchasesSummary(range: DateRange): Observable<PurchasesSummaryReport> {
    return this.http.get<PurchasesSummaryReport>(`${this.baseUrl}/purchases-summary`, {
      params: this.rangeParams(range),
    });
  }

  purchasesBySupplier(range: DateRange): Observable<PurchasesBySupplierReport[]> {
    return this.http.get<PurchasesBySupplierReport[]>(`${this.baseUrl}/purchases-by-supplier`, {
      params: this.rangeParams(range),
    });
  }

  cashSummary(range: DateRange): Observable<CashSummaryReport> {
    return this.http.get<CashSummaryReport>(`${this.baseUrl}/cash-summary`, {
      params: this.rangeParams(range),
    });
  }

  cashByCategory(range: DateRange): Observable<CashByCategoryReport[]> {
    return this.http.get<CashByCategoryReport[]>(`${this.baseUrl}/cash-by-category`, {
      params: this.rangeParams(range),
    });
  }

  estimatedProfit(range: DateRange): Observable<EstimatedProfitReport> {
    return this.http.get<EstimatedProfitReport>(`${this.baseUrl}/estimated-profit`, {
      params: this.rangeParams(range),
    });
  }
}
