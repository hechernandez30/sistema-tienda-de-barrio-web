import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PurchaseService } from '../../../core/services/purchase.service';
import {
  PAYMENT_METHOD_LABELS,
  PURCHASE_STATUS_LABELS,
  PurchaseDetail,
} from '../../../core/models/purchase.model';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../../shared/components/confirm-dialog/confirm-dialog.component';

export interface PurchaseDetailDialogData {
  purchaseId: string;
}

@Component({
  selector: 'app-purchase-detail-dialog',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './purchase-detail-dialog.component.html',
})
export class PurchaseDetailDialogComponent {
  private readonly purchaseService = inject(PurchaseService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<PurchaseDetailDialogComponent>);
  private readonly data = inject<PurchaseDetailDialogData>(MAT_DIALOG_DATA);

  readonly purchase = signal<PurchaseDetail | null>(null);
  readonly loading = signal(true);
  readonly processing = signal(false);
  /** Marca si hubo algún cambio para que el listado se recargue al cerrar. */
  private changed = false;

  readonly statusLabels = PURCHASE_STATUS_LABELS;
  readonly paymentLabels = PAYMENT_METHOD_LABELS;

  constructor() {
    this.loadDetail();
  }

  loadDetail(): void {
    this.loading.set(true);
    this.purchaseService.getById(this.data.purchaseId).subscribe({
      next: (detail) => {
        this.purchase.set(detail);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('No se pudo cargar la compra', 'Cerrar', { duration: 4000 });
      },
    });
  }

  statusBadgeClass(): string {
    switch (this.purchase()?.status) {
      case 'CONFIRMED':
        return 'bg-emerald-100 text-emerald-700';
      case 'CANCELLED':
        return 'bg-rose-100 text-rose-700';
      default:
        return 'bg-amber-100 text-amber-700';
    }
  }

  confirmPurchase(): void {
    const purchase = this.purchase();
    if (!purchase) {
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Confirmar compra',
        message: 'Al confirmar la compra, se actualizará el inventario de los productos incluidos.',
        confirmText: 'Confirmar',
      } as ConfirmDialogData,
      autoFocus: false,
      width: '26rem',
      maxWidth: '92vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((ok) => {
      if (!ok) {
        return;
      }
      this.processing.set(true);
      this.purchaseService.confirm(purchase.id).subscribe({
        next: (updated) => {
          this.processing.set(false);
          this.changed = true;
          this.purchase.set(updated);
          this.snackBar.open('Compra confirmada. Inventario actualizado.', 'Cerrar', { duration: 3500 });
        },
        error: (error) => {
          this.processing.set(false);
          const message = error?.error?.message ?? 'No se pudo confirmar la compra';
          this.snackBar.open(message, 'Cerrar', { duration: 4500 });
        },
      });
    });
  }

  cancelPurchase(): void {
    const purchase = this.purchase();
    if (!purchase) {
      return;
    }
    const wasConfirmed = purchase.status === 'CONFIRMED';
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancelar compra',
        message: wasConfirmed
          ? 'Esta compra está confirmada. Al cancelarla, el sistema intentará revertir el inventario ingresado.'
          : '¿Deseas cancelar esta compra?',
        confirmText: 'Cancelar compra',
        cancelText: 'Volver',
        danger: true,
      } as ConfirmDialogData,
      autoFocus: false,
      width: '26rem',
      maxWidth: '92vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((ok) => {
      if (!ok) {
        return;
      }
      this.processing.set(true);
      this.purchaseService.cancel(purchase.id).subscribe({
        next: (updated) => {
          this.processing.set(false);
          this.changed = true;
          this.purchase.set(updated);
          this.snackBar.open('Compra cancelada correctamente', 'Cerrar', { duration: 3500 });
        },
        error: (error) => {
          this.processing.set(false);
          const message =
            error?.error?.message ??
            'No se pudo cancelar la compra porque algunos productos ya no tienen stock suficiente para revertir el ingreso.';
          this.snackBar.open(message, 'Cerrar', { duration: 5000 });
        },
      });
    });
  }

  close(): void {
    this.dialogRef.close(this.changed);
  }
}
