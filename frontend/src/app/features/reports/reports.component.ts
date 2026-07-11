import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin, of, Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { DateRange, ReportService } from '../../core/services/report.service';
import { InventoryService } from '../../core/services/inventory.service';
import { CatalogService } from '../../core/services/catalog.service';
import { Category } from '../../core/models/catalog.model';
import { ProductLot } from '../../core/models/inventory.model';
import {
  CashByCategoryReport,
  CashSummaryReport,
  DailySalesReport,
  EstimatedProfitReport,
  InventorySummaryReport,
  LowStockReport,
  MOVEMENT_TYPE_LABELS,
  PAYMENT_METHOD_LABELS,
  PaymentMethod,
  PurchasesBySupplierReport,
  PurchasesSummaryReport,
  SalesByCategoryReport,
  SalesByPaymentMethodReport,
  SalesSummaryReport,
  TopProductReport,
} from '../../core/models/report.model';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe],
  templateUrl: './reports.component.html',
})
export class ReportsComponent implements OnInit {
  private readonly reportService = inject(ReportService);
  private readonly inventoryService = inject(InventoryService);
  private readonly catalogService = inject(CatalogService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly paymentLabels = PAYMENT_METHOD_LABELS;
  readonly movementLabels = MOVEMENT_TYPE_LABELS;

  readonly filterForm = this.fb.nonNullable.group({
    from: this.today(),
    to: this.today(),
  });

  /** Filtro exclusivo del reporte por categoría de producto (catálogo product_categories). */
  readonly categoryFilter = signal<string>('ALL');

  readonly productCategories = signal<Category[]>([]);
  readonly loadingCategoryReport = signal(false);

  readonly loading = signal(false);
  readonly errored = signal(false);

  readonly salesSummary = signal<SalesSummaryReport | null>(null);
  readonly salesByPayment = signal<SalesByPaymentMethodReport[]>([]);
  readonly dailySales = signal<DailySalesReport[]>([]);
  readonly salesByCategory = signal<SalesByCategoryReport[]>([]);
  readonly topProducts = signal<TopProductReport[]>([]);
  readonly lowStock = signal<LowStockReport[]>([]);
  readonly inventorySummary = signal<InventorySummaryReport | null>(null);
  readonly purchasesSummary = signal<PurchasesSummaryReport | null>(null);
  readonly purchasesBySupplier = signal<PurchasesBySupplierReport[]>([]);
  readonly cashSummary = signal<CashSummaryReport | null>(null);
  readonly cashByCategory = signal<CashByCategoryReport[]>([]);
  readonly estimatedProfit = signal<EstimatedProfitReport | null>(null);
  readonly expiringLots = signal<ProductLot[]>([]);
  readonly expiredLots = signal<ProductLot[]>([]);

  readonly EXPIRING_DAYS = 30;

  // Máximos para barras proporcionales.
  readonly maxPaymentAmount = computed(() =>
    Math.max(0, ...this.salesByPayment().map((p) => p.totalAmount)),
  );
  readonly maxDailyAmount = computed(() =>
    Math.max(0, ...this.dailySales().map((d) => d.totalAmount)),
  );
  readonly maxCategoryAmount = computed(() =>
    Math.max(0, ...this.salesByCategory().map((c) => c.totalAmount)),
  );
  readonly selectedCategorySummary = computed(() => {
    const rows = this.salesByCategory();
    if (this.categoryFilter() === 'ALL' || rows.length !== 1) {
      return null;
    }
    return rows[0];
  });
  readonly maxTopProductQty = computed(() =>
    Math.max(0, ...this.topProducts().map((p) => p.quantitySold)),
  );
  readonly maxSupplierAmount = computed(() =>
    Math.max(0, ...this.purchasesBySupplier().map((s) => s.totalAmount)),
  );

  ngOnInit(): void {
    this.catalogService.listCategories().subscribe({
      next: (categories) => this.productCategories.set(categories),
      error: () =>
        this.snackBar.open('No se pudo cargar el catálogo de categorías', 'Cerrar', { duration: 4000 }),
    });
    this.loadAll();
  }

  private today(): string {
    return this.toIso(new Date());
  }

  private toIso(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  setToday(): void {
    const today = this.today();
    this.filterForm.setValue({ from: today, to: today });
    this.loadAll();
  }

  setThisWeek(): void {
    const now = new Date();
    const dayOfWeek = now.getDay();
    const mondayOffset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    const monday = new Date(now);
    monday.setDate(now.getDate() + mondayOffset);
    this.filterForm.setValue({ from: this.toIso(monday), to: this.toIso(now) });
    this.loadAll();
  }

  setThisMonth(): void {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    this.filterForm.setValue({ from: this.toIso(firstDay), to: this.toIso(now) });
    this.loadAll();
  }

  applyFilters(): void {
    this.loadAll();
  }

  onCategoryFilterChange(value: string): void {
    this.categoryFilter.set(value);
    this.loadCategoryReport();
  }

  private categoryReportFilters() {
    const value = this.categoryFilter();
    if (value === 'ALL') {
      return {};
    }
    if (value === 'NONE') {
      return { uncategorizedOnly: true };
    }
    return { categoryId: value };
  }

  loadCategoryReport(): void {
    const range: DateRange = {
      from: this.filterForm.controls.from.value,
      to: this.filterForm.controls.to.value,
    };

    this.loadingCategoryReport.set(true);
    this.reportService.salesByCategory(range, this.categoryReportFilters()).subscribe({
      next: (rows) => {
        this.salesByCategory.set(rows);
        this.loadingCategoryReport.set(false);
      },
      error: () => {
        this.loadingCategoryReport.set(false);
        this.snackBar.open('No se pudo cargar el reporte por categoría', 'Cerrar', { duration: 4000 });
      },
    });
  }

  percent(value: number, max: number): number {
    if (!max || max <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((value / max) * 100));
  }

  paymentLabel(method: PaymentMethod): string {
    return this.paymentLabels[method] ?? method;
  }

  stockDiff(item: LowStockReport): number {
    return item.currentStock - item.minStock;
  }

  loadAll(): void {
    const range: DateRange = {
      from: this.filterForm.controls.from.value,
      to: this.filterForm.controls.to.value,
    };

    this.loading.set(true);
    this.errored.set(false);

    const withFallback = <T>(request$: Observable<T>, fallback: T) =>
      request$.pipe(catchError(() => of(fallback)));

    forkJoin({
      salesSummary: this.reportService.salesSummary(range),
      salesByPayment: this.reportService.salesByPaymentMethod(range),
      dailySales: this.reportService.dailySales(range),
      salesByCategory: withFallback(
        this.reportService.salesByCategory(range, this.categoryReportFilters()),
        [],
      ),
      topProducts: this.reportService.topProducts(range, 10),
      lowStock: this.reportService.lowStock(),
      inventorySummary: this.reportService.inventorySummary(),
      purchasesSummary: this.reportService.purchasesSummary(range),
      purchasesBySupplier: this.reportService.purchasesBySupplier(range),
      cashSummary: this.reportService.cashSummary(range),
      cashByCategory: this.reportService.cashByCategory(range),
      estimatedProfit: this.reportService.estimatedProfit(range),
      expiringLots: withFallback(this.inventoryService.expiringLots(this.EXPIRING_DAYS), []),
      expiredLots: withFallback(this.inventoryService.expiredLots(), []),
    }).subscribe({
      next: (data) => {
        this.salesSummary.set(data.salesSummary);
        this.salesByPayment.set(data.salesByPayment);
        this.dailySales.set(data.dailySales);
        this.salesByCategory.set(data.salesByCategory);
        this.topProducts.set(data.topProducts);
        this.lowStock.set(data.lowStock);
        this.inventorySummary.set(data.inventorySummary);
        this.purchasesSummary.set(data.purchasesSummary);
        this.purchasesBySupplier.set(data.purchasesBySupplier);
        this.cashSummary.set(data.cashSummary);
        this.cashByCategory.set(data.cashByCategory);
        this.estimatedProfit.set(data.estimatedProfit);
        this.expiringLots.set(data.expiringLots);
        this.expiredLots.set(data.expiredLots);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errored.set(true);
        this.snackBar.open('No se pudieron cargar los reportes. Intenta nuevamente.', 'Cerrar', {
          duration: 4500,
        });
      },
    });
  }
}
