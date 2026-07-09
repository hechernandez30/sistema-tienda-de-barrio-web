import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

import { PurchaseService } from '../../../core/services/purchase.service';
import { SupplierService } from '../../../core/services/supplier.service';
import { ProductService } from '../../../core/services/product.service';
import { SupplierListItem } from '../../../core/models/supplier.model';
import { ProductPos } from '../../../core/models/product.model';
import {
  PaymentMethod,
  PurchaseCreateRequest,
  PurchaseItemRequest,
} from '../../../core/models/purchase.model';

interface EditableItem {
  productId: string;
  productName: string;
  barcode?: string | null;
  quantity: number;
  unitCost: number;
  tracksExpiration: boolean;
  expirationDate: string;
  lotCode: string;
}

@Component({
  selector: 'app-purchase-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './purchase-form-dialog.component.html',
})
export class PurchaseFormDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly purchaseService = inject(PurchaseService);
  private readonly supplierService = inject(SupplierService);
  private readonly productService = inject(ProductService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<PurchaseFormDialogComponent>);

  readonly saving = signal(false);
  readonly suppliers = signal<SupplierListItem[]>([]);
  readonly items = signal<EditableItem[]>([]);

  readonly searching = signal(false);
  readonly results = signal<ProductPos[]>([]);
  readonly productSearchControl = new FormControl('', { nonNullable: true });

  readonly paymentMethods: { value: PaymentMethod; label: string }[] = [
    { value: 'CASH', label: 'Efectivo' },
    { value: 'TRANSFER', label: 'Transferencia' },
    { value: 'CARD', label: 'Tarjeta' },
  ];

  readonly form = this.fb.nonNullable.group({
    supplierId: ['', [Validators.required]],
    purchaseDate: [''],
    isPaid: [false],
    paymentMethod: ['CASH' as PaymentMethod],
    notes: [''],
  });

  readonly subtotal = computed(() => this.items().reduce((acc, item) => acc + item.quantity * item.unitCost, 0));
  readonly hasTrackingItems = computed(() => this.items().some((item) => item.tracksExpiration));

  constructor() {
    this.productSearchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((term) => {
          const clean = term.trim();
          if (clean.length < 2) {
            this.searching.set(false);
            return of<ProductPos[]>([]);
          }
          this.searching.set(true);
          return this.productService.search(clean);
        }),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (products) => {
          this.results.set(products);
          this.searching.set(false);
        },
        error: () => {
          this.results.set([]);
          this.searching.set(false);
        },
      });
  }

  ngOnInit(): void {
    this.supplierService.list().subscribe({
      next: (list) => this.suppliers.set(list.filter((s) => s.active)),
      error: () => this.snackBar.open('No se pudieron cargar los proveedores', 'Cerrar', { duration: 4000 }),
    });
  }

  addProduct(product: ProductPos): void {
    const existing = this.items().find((i) => i.productId === product.id);
    if (existing) {
      this.items.update((items) =>
        items.map((i) => (i.productId === product.id ? { ...i, quantity: i.quantity + 1 } : i)),
      );
    } else {
      this.items.update((items) => [
        ...items,
        {
          productId: product.id,
          productName: product.name,
          barcode: product.barcode,
          quantity: 1,
          unitCost: product.salePrice ?? 0,
          tracksExpiration: !!product.tracksExpiration,
          expirationDate: '',
          lotCode: '',
        },
      ]);
    }
    this.results.set([]);
    this.productSearchControl.setValue('', { emitEvent: false });
  }

  updateQuantity(index: number, value: string): void {
    const quantity = Number(value);
    this.items.update((items) =>
      items.map((item, i) => (i === index ? { ...item, quantity: Number.isFinite(quantity) ? quantity : 0 } : item)),
    );
  }

  updateUnitCost(index: number, value: string): void {
    const unitCost = Number(value);
    this.items.update((items) =>
      items.map((item, i) => (i === index ? { ...item, unitCost: Number.isFinite(unitCost) ? unitCost : 0 } : item)),
    );
  }

  removeItem(index: number): void {
    this.items.update((items) => items.filter((_, i) => i !== index));
  }

  updateExpirationDate(index: number, value: string): void {
    this.items.update((items) =>
      items.map((item, i) => (i === index ? { ...item, expirationDate: value } : item)),
    );
  }

  updateLotCode(index: number, value: string): void {
    this.items.update((items) =>
      items.map((item, i) => (i === index ? { ...item, lotCode: value } : item)),
    );
  }

  save(): void {
    if (this.saving()) {
      return;
    }
    if (this.form.controls.supplierId.invalid) {
      this.form.markAllAsTouched();
      this.snackBar.open('Debes seleccionar un proveedor', 'Cerrar', { duration: 3000 });
      return;
    }
    if (this.items().length === 0) {
      this.snackBar.open('Agrega al menos un producto a la compra', 'Cerrar', { duration: 3000 });
      return;
    }
    const invalidLine = this.items().some((i) => i.quantity <= 0 || i.unitCost < 0);
    if (invalidLine) {
      this.snackBar.open('Revisa las cantidades y costos de los productos', 'Cerrar', { duration: 3500 });
      return;
    }
    const missingExpiration = this.items().some((i) => i.tracksExpiration && !i.expirationDate);
    if (missingExpiration) {
      this.snackBar.open('Los productos con vencimiento requieren fecha de vencimiento en cada línea', 'Cerrar', {
        duration: 4000,
      });
      return;
    }

    const raw = this.form.getRawValue();
    const itemsPayload: PurchaseItemRequest[] = this.items().map((i) => ({
      productId: i.productId,
      quantity: i.quantity,
      unitCost: i.unitCost,
      expirationDate: i.tracksExpiration ? i.expirationDate : null,
      lotCode: i.tracksExpiration && i.lotCode.trim() ? i.lotCode.trim() : null,
    }));

    const payload: PurchaseCreateRequest = {
      supplierId: raw.supplierId,
      purchaseDate: raw.purchaseDate ? new Date(raw.purchaseDate).toISOString() : null,
      isPaid: raw.isPaid,
      paymentMethod: raw.isPaid ? raw.paymentMethod : null,
      notes: raw.notes?.trim() ? raw.notes.trim() : null,
      items: itemsPayload,
    };

    this.saving.set(true);
    this.purchaseService.create(payload).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Compra creada en borrador', 'Cerrar', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.saving.set(false);
        const message = error?.error?.message ?? 'No se pudo crear la compra';
        this.snackBar.open(message, 'Cerrar', { duration: 4000 });
      },
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
