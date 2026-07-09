import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';

import { InventoryService } from '../../../core/services/inventory.service';
import { ProductService } from '../../../core/services/product.service';
import { InventoryAdjustmentRequest } from '../../../core/models/inventory.model';
import { ProductPos } from '../../../core/models/product.model';

export type AdjustmentMode = 'in' | 'out';

export interface SelectedProduct {
  id: string;
  name: string;
  barcode?: string | null;
  currentStock?: number | null;
  tracksExpiration?: boolean;
}

export interface AdjustmentDialogData {
  mode: AdjustmentMode;
  product?: SelectedProduct;
}

@Component({
  selector: 'app-inventory-adjustment-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './inventory-adjustment-dialog.component.html',
})
export class InventoryAdjustmentDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly inventoryService = inject(InventoryService);
  private readonly productService = inject(ProductService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<InventoryAdjustmentDialogComponent>);
  readonly data = inject<AdjustmentDialogData>(MAT_DIALOG_DATA);

  readonly isIncoming = this.data.mode === 'in';
  readonly saving = signal(false);
  readonly searching = signal(false);
  readonly results = signal<ProductPos[]>([]);
  readonly selected = signal<SelectedProduct | null>(this.data.product ?? null);

  readonly searchControl = new FormControl('', { nonNullable: true });

  readonly form = this.fb.nonNullable.group({
    quantity: [null as number | null, [Validators.required, Validators.min(0.001)]],
    unitCost: [null as number | null, [Validators.min(0)]],
    expirationDate: [''],
    lotCode: [''],
    notes: [''],
  });

  constructor() {
    this.searchControl.valueChanges
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

  selectProduct(product: ProductPos): void {
    this.selected.set({
      id: product.id,
      name: product.name,
      barcode: product.barcode,
      currentStock: product.currentStock,
      tracksExpiration: product.tracksExpiration,
    });
    this.results.set([]);
    this.searchControl.setValue('', { emitEvent: false });
  }

  clearSelection(): void {
    this.selected.set(null);
  }

  save(): void {
    const product = this.selected();
    if (!product) {
      this.snackBar.open('Debes seleccionar un producto', 'Cerrar', { duration: 3000 });
      return;
    }
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request: InventoryAdjustmentRequest = {
      productId: product.id,
      quantity: Number(raw.quantity),
      notes: raw.notes?.trim() ? raw.notes.trim() : null,
    };
    if (this.isIncoming && raw.unitCost !== null && raw.unitCost !== undefined) {
      request.unitCost = Number(raw.unitCost);
    }
    if (this.isIncoming && product.tracksExpiration) {
      if (!raw.expirationDate) {
        this.snackBar.open('La fecha de vencimiento es obligatoria para este producto', 'Cerrar', {
          duration: 4000,
        });
        return;
      }
      request.expirationDate = raw.expirationDate;
      request.lotCode = raw.lotCode?.trim() ? raw.lotCode.trim() : null;
    }

    this.saving.set(true);
    const request$ = this.isIncoming
      ? this.inventoryService.adjustIn(request)
      : this.inventoryService.adjustOut(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open(
          this.isIncoming ? 'Ajuste de entrada registrado' : 'Ajuste de salida registrado',
          'Cerrar',
          { duration: 3000 },
        );
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.saving.set(false);
        this.snackBar.open(this.resolveError(error), 'Cerrar', { duration: 4500 });
      },
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  private resolveError(error: unknown): string {
    const err = error as { status?: number; error?: { message?: string } };
    const backendMessage = err?.error?.message;
    if (!this.isIncoming && (err?.status === 400 || err?.status === 409)) {
      return backendMessage ?? 'No hay stock suficiente para realizar la salida.';
    }
    return backendMessage ?? 'No se pudo registrar el ajuste';
  }
}
