import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../core/services/auth.service';
import { SalesService } from '../../../core/services/sales.service';
import { ReceiptService } from '../../../core/services/receipt.service';
import {
  PAYMENT_METHOD_LABELS,
  SALE_STATUS_LABELS,
  SaleDetail,
} from '../../../core/models/sale.model';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../../shared/components/confirm-dialog/confirm-dialog.component';

export interface SaleDetailDialogData {
  saleId: string;
}

@Component({
  selector: 'app-sale-detail-dialog',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './sale-detail-dialog.component.html',
})
export class SaleDetailDialogComponent {
  private readonly salesService = inject(SalesService);
  private readonly receiptService = inject(ReceiptService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<SaleDetailDialogComponent>);
  private readonly data = inject<SaleDetailDialogData>(MAT_DIALOG_DATA);

  readonly sale = signal<SaleDetail | null>(null);
  readonly loading = signal(true);
  readonly processing = signal(false);
  private changed = false;

  readonly paymentLabels = PAYMENT_METHOD_LABELS;
  readonly statusLabels = SALE_STATUS_LABELS;

  readonly canCancel = computed(
    () => this.authService.hasRole('ADMIN') && this.sale()?.status === 'COMPLETED',
  );

  constructor() {
    this.salesService.getById(this.data.saleId).subscribe({
      next: (detail) => {
        this.sale.set(detail);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('No se pudo cargar la venta', 'Cerrar', { duration: 4000 });
      },
    });
  }

  cancelSale(): void {
    const sale = this.sale();
    if (!sale) {
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancelar venta',
        message: 'Al cancelar la venta, el stock será devuelto y se registrará la anulación en caja.',
        confirmText: 'Cancelar venta',
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
      this.salesService.cancel(sale.id).subscribe({
        next: (updated) => {
          this.processing.set(false);
          this.changed = true;
          this.sale.set(updated);
          this.snackBar.open('Venta cancelada. Stock e ingreso revertidos.', 'Cerrar', { duration: 3500 });
        },
        error: (error) => {
          this.processing.set(false);
          const message = error?.error?.message ?? 'No se pudo cancelar la venta';
          this.snackBar.open(message, 'Cerrar', { duration: 4500 });
        },
      });
    });
  }

  printReceipt(): void {
    const sale = this.sale();
    if (!sale) {
      return;
    }
    const ok = this.receiptService.print(sale);
    if (!ok) {
      this.snackBar.open('Habilita las ventanas emergentes para imprimir el comprobante.', 'Cerrar', {
        duration: 5000,
      });
    }
  }

  close(): void {
    this.dialogRef.close(this.changed);
  }
}
