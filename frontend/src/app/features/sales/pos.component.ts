import {
  AfterViewInit,
  Component,
  ElementRef,
  OnInit,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

import { AuthService } from '../../core/services/auth.service';
import { SalesService } from '../../core/services/sales.service';
import { ReceiptService } from '../../core/services/receipt.service';
import { ProductService } from '../../core/services/product.service';
import { CustomerService } from '../../core/services/customer.service';
import { CashService } from '../../core/services/cash.service';
import { ProductPos } from '../../core/models/product.model';
import { CustomerDetail, CustomerListItem } from '../../core/models/customer.model';
import {
  CustomerDialogData,
  CustomerFormDialogComponent,
} from '../customers/customer-form-dialog/customer-form-dialog.component';
import {
  PAYMENT_METHOD_LABELS,
  PaymentMethod,
  PosCartItem,
  SALE_STATUS_LABELS,
  SaleCreateRequest,
  SaleListItem,
} from '../../core/models/sale.model';
import {
  SaleDetailDialogComponent,
  SaleDetailDialogData,
} from './sale-detail-dialog/sale-detail-dialog.component';

@Component({
  selector: 'app-pos',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe, RouterLink],
  templateUrl: './pos.component.html',
})
export class PosComponent implements OnInit, AfterViewInit {
  private readonly salesService = inject(SalesService);
  private readonly receiptService = inject(ReceiptService);
  private readonly productService = inject(ProductService);
  private readonly customerService = inject(CustomerService);
  private readonly cashService = inject(CashService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  @ViewChild('scanInput') scanInput?: ElementRef<HTMLInputElement>;

  readonly paymentLabels = PAYMENT_METHOD_LABELS;
  readonly statusLabels = SALE_STATUS_LABELS;
  readonly paymentMethods: { value: PaymentMethod; label: string }[] = [
    { value: 'CASH', label: 'Efectivo' },
    { value: 'TRANSFER', label: 'Transferencia' },
    { value: 'CARD', label: 'Tarjeta' },
  ];

  readonly isAdmin = computed(() => this.authService.hasRole('ADMIN'));

  // Estado de caja
  readonly cashOpen = signal(false);
  readonly loadingCash = signal(false);

  // Carrito y venta
  readonly cart = signal<PosCartItem[]>([]);
  readonly paymentMethod = signal<PaymentMethod>('CASH');
  readonly registering = signal(false);

  // Escaneo / búsqueda
  readonly scanControl = new FormControl('', { nonNullable: true });
  readonly searchResults = signal<ProductPos[]>([]);
  readonly searchingProduct = signal(false);
  readonly liveSearching = signal(false);
  readonly notFound = signal(false);

  // Cliente
  readonly customerSearchControl = new FormControl('', { nonNullable: true });
  readonly customerResults = signal<CustomerListItem[]>([]);
  readonly searchingCustomer = signal(false);
  readonly selectedCustomer = signal<CustomerListItem | null>(null);

  // Ventas del día
  readonly todaySales = signal<SaleListItem[]>([]);
  readonly loadingToday = signal(false);

  readonly distinctCount = computed(() => this.cart().length);
  readonly totalUnits = computed(() => this.cart().reduce((acc, i) => acc + i.quantity, 0));
  readonly subtotal = computed(() => this.cart().reduce((acc, i) => acc + i.quantity * i.salePrice, 0));
  readonly hasStockIssue = computed(() => this.cart().some((i) => i.quantity > i.currentStock || i.quantity <= 0));
  readonly canRegister = computed(
    () => this.cashOpen() && this.cart().length > 0 && !this.hasStockIssue() && !this.registering(),
  );

  constructor() {
    // Búsqueda de cliente con debounce.
    this.customerSearchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((term) => {
          const clean = term.trim();
          if (clean.length < 2) {
            this.searchingCustomer.set(false);
            return of<CustomerListItem[]>([]);
          }
          this.searchingCustomer.set(true);
          return this.customerService.search(clean).pipe(catchError(() => of<CustomerListItem[]>([])));
        }),
        takeUntilDestroyed(),
      )
      .subscribe((results) => {
        this.customerResults.set(results);
        this.searchingCustomer.set(false);
      });

    // Búsqueda de producto por coincidencia mientras se escribe.
    this.scanControl.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((value) => {
          const term = value.trim();
          this.notFound.set(false);
          if (term.length < 2) {
            this.liveSearching.set(false);
            this.searchResults.set([]);
            return of<ProductPos[]>([]);
          }
          this.liveSearching.set(true);
          return this.productService.search(term).pipe(catchError(() => of<ProductPos[]>([])));
        }),
        takeUntilDestroyed(),
      )
      .subscribe((results) => {
        this.liveSearching.set(false);
        this.searchResults.set(results);
      });
  }

  ngOnInit(): void {
    this.loadCashStatus();
    this.loadToday();
  }

  ngAfterViewInit(): void {
    this.focusScan();
  }

  // ---------------------------------------------------------------
  // Caja
  // ---------------------------------------------------------------
  loadCashStatus(): void {
    this.loadingCash.set(true);
    this.cashService.getCurrentSession().subscribe({
      next: (summary) => {
        this.cashOpen.set(!!summary?.cashSessionId);
        this.loadingCash.set(false);
      },
      error: () => {
        this.cashOpen.set(false);
        this.loadingCash.set(false);
      },
    });
  }

  // ---------------------------------------------------------------
  // Escaneo / búsqueda de producto
  // ---------------------------------------------------------------
  onScanEnter(): void {
    const term = this.scanControl.value.trim();
    if (!term || this.searchingProduct()) {
      return;
    }
    this.notFound.set(false);
    this.searchingProduct.set(true);

    // Primero intenta por código de barras exacto.
    this.productService.getByBarcode(term).subscribe({
      next: (product) => {
        this.searchingProduct.set(false);
        this.addToCart(product);
      },
      error: () => {
        // Si no existe por código, busca por nombre.
        this.productService.search(term).subscribe({
          next: (results) => {
            this.searchingProduct.set(false);
            if (results.length === 0) {
              this.notFound.set(true);
              this.searchResults.set([]);
            } else if (results.length === 1) {
              this.addToCart(results[0]);
            } else {
              this.searchResults.set(results);
            }
          },
          error: () => {
            this.searchingProduct.set(false);
            this.notFound.set(true);
          },
        });
      },
    });
  }

  selectSearchResult(product: ProductPos): void {
    this.addToCart(product);
  }

  private addToCart(product: ProductPos): void {
    if (Number(product.currentStock) <= 0) {
      this.snackBar.open(`"${product.name}" no tiene stock disponible`, 'Cerrar', { duration: 3500 });
      this.resetScan();
      return;
    }

    const existing = this.cart().find((i) => i.productId === product.id);
    if (existing) {
      if (existing.quantity + 1 > existing.currentStock) {
        this.snackBar.open(`Stock insuficiente para "${product.name}"`, 'Cerrar', { duration: 3500 });
      } else {
        this.cart.update((items) =>
          items.map((i) => (i.productId === product.id ? { ...i, quantity: i.quantity + 1 } : i)),
        );
      }
    } else {
      this.cart.update((items) => [
        ...items,
        {
          productId: product.id,
          barcode: product.barcode,
          productName: product.name,
          salePrice: Number(product.salePrice ?? 0),
          currentStock: Number(product.currentStock ?? 0),
          quantity: 1,
        },
      ]);
    }
    this.resetScan();
  }

  private resetScan(): void {
    this.scanControl.setValue('');
    this.searchResults.set([]);
    this.notFound.set(false);
    this.focusScan();
  }

  focusScan(): void {
    setTimeout(() => this.scanInput?.nativeElement.focus(), 0);
  }

  // ---------------------------------------------------------------
  // Carrito
  // ---------------------------------------------------------------
  updateQuantity(index: number, value: string): void {
    const quantity = Number(value);
    this.cart.update((items) =>
      items.map((item, i) =>
        i === index ? { ...item, quantity: Number.isFinite(quantity) ? quantity : 0 } : item,
      ),
    );
  }

  increment(index: number): void {
    this.cart.update((items) =>
      items.map((item, i) =>
        i === index && item.quantity < item.currentStock ? { ...item, quantity: item.quantity + 1 } : item,
      ),
    );
  }

  decrement(index: number): void {
    this.cart.update((items) =>
      items.map((item, i) => (i === index && item.quantity > 1 ? { ...item, quantity: item.quantity - 1 } : item)),
    );
  }

  removeItem(index: number): void {
    this.cart.update((items) => items.filter((_, i) => i !== index));
    this.focusScan();
  }

  lineExceedsStock(item: PosCartItem): boolean {
    return item.quantity > item.currentStock;
  }

  // ---------------------------------------------------------------
  // Cliente
  // ---------------------------------------------------------------
  selectCustomer(customer: CustomerListItem): void {
    this.selectedCustomer.set(customer);
    this.customerResults.set([]);
    this.customerSearchControl.setValue('', { emitEvent: false });
  }

  clearCustomer(): void {
    this.selectedCustomer.set(null);
  }

  /**
   * Abre el formulario existente de clientes en modo crear. Si se crea el cliente,
   * lo deja seleccionado en la tarjeta Cliente del punto de venta.
   */
  openCreateCustomer(): void {
    const ref = this.dialog.open(CustomerFormDialogComponent, {
      data: { mode: 'create' } as CustomerDialogData,
      autoFocus: false,
      width: '640px',
      maxWidth: '96vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((result: CustomerDetail | boolean | undefined) => {
      if (result && typeof result === 'object' && 'id' in result) {
        this.customerResults.set([]);
        this.customerSearchControl.setValue('', { emitEvent: false });
        this.selectedCustomer.set({
          id: result.id,
          fullName: result.fullName,
          nit: result.nit ?? null,
          phone: result.phone ?? null,
          email: result.email ?? null,
          active: result.active,
        });
      }
    });
  }

  // ---------------------------------------------------------------
  // Registrar venta
  // ---------------------------------------------------------------
  setPaymentMethod(method: PaymentMethod): void {
    this.paymentMethod.set(method);
  }

  registerSale(): void {
    if (!this.cashOpen()) {
      this.snackBar.open('No hay caja abierta. Abre caja antes de vender.', 'Cerrar', { duration: 4000 });
      return;
    }
    if (this.cart().length === 0) {
      this.snackBar.open('Escanea o busca productos para iniciar una venta.', 'Cerrar', { duration: 3500 });
      return;
    }
    if (this.hasStockIssue()) {
      this.snackBar.open('Revisa las cantidades: no pueden superar el stock disponible.', 'Cerrar', { duration: 4000 });
      return;
    }

    const payload: SaleCreateRequest = {
      customerId: this.selectedCustomer()?.id ?? null,
      paymentMethod: this.paymentMethod(),
      notes: null,
      items: this.cart().map((i) => ({ productId: i.productId, quantity: i.quantity })),
    };

    this.registering.set(true);
    this.salesService.create(payload).subscribe({
      next: () => {
        this.registering.set(false);
        this.snackBar.open('Venta registrada correctamente', 'Cerrar', { duration: 3000 });
        this.cart.set([]);
        this.clearCustomer();
        this.paymentMethod.set('CASH');
        this.loadToday();
        this.loadCashStatus();
        this.focusScan();
      },
      error: (error) => {
        this.registering.set(false);
        this.snackBar.open(this.resolveSaleError(error), 'Cerrar', { duration: 5000 });
      },
    });
  }

  private resolveSaleError(error: unknown): string {
    const err = error as { status?: number; error?: { code?: string; message?: string } };
    const code = err?.error?.code;
    if (code === 'NO_OPEN_CASH_SESSION') {
      return 'No hay caja abierta. Abre caja antes de vender.';
    }
    if (code === 'INSUFFICIENT_STOCK') {
      return 'No hay stock suficiente para uno o más productos.';
    }
    return err?.error?.message ?? 'No se pudo registrar la venta. Intenta nuevamente.';
  }

  // ---------------------------------------------------------------
  // Ventas del día
  // ---------------------------------------------------------------
  loadToday(): void {
    this.loadingToday.set(true);
    this.salesService.today().subscribe({
      next: (sales) => {
        this.todaySales.set(sales);
        this.loadingToday.set(false);
      },
      error: () => {
        this.loadingToday.set(false);
        this.snackBar.open('No se pudieron cargar las ventas del día', 'Cerrar', { duration: 4000 });
      },
    });
  }

  openDetail(sale: SaleListItem): void {
    const ref = this.dialog.open(SaleDetailDialogComponent, {
      data: { saleId: sale.id } as SaleDetailDialogData,
      autoFocus: false,
      width: '760px',
      maxWidth: '96vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((changed) => {
      if (changed) {
        this.loadToday();
        this.loadCashStatus();
      }
    });
  }

  printReceipt(sale: SaleListItem): void {
    this.salesService.getById(sale.id).subscribe({
      next: (detail) => {
        const ok = this.receiptService.print(detail);
        if (!ok) {
          this.snackBar.open(
            'Habilita las ventanas emergentes para imprimir el comprobante.',
            'Cerrar',
            { duration: 5000 },
          );
        }
      },
      error: () =>
        this.snackBar.open('No se pudo generar el comprobante', 'Cerrar', { duration: 4000 }),
    });
  }
}
