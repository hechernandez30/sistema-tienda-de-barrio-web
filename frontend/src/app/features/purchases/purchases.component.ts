import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { PurchaseService } from '../../core/services/purchase.service';
import {
  PURCHASE_STATUS_LABELS,
  PurchaseListItem,
  PurchaseStatus,
} from '../../core/models/purchase.model';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { PurchaseFormDialogComponent } from './purchase-form-dialog/purchase-form-dialog.component';
import {
  PurchaseDetailDialogComponent,
  PurchaseDetailDialogData,
} from './purchase-detail-dialog/purchase-detail-dialog.component';

@Component({
  selector: 'app-purchases',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe],
  templateUrl: './purchases.component.html',
})
export class PurchasesComponent implements OnInit {
  private readonly purchaseService = inject(PurchaseService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly statusLabels = PURCHASE_STATUS_LABELS;
  readonly purchases = signal<PurchaseListItem[]>([]);
  readonly loading = signal(false);
  readonly searchTerm = signal('');
  readonly searchControl = new FormControl('', { nonNullable: true });

  readonly filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const list = this.purchases();
    if (!term) {
      return list;
    }
    return list.filter((p) => {
      const statusLabel = this.statusLabels[p.status].toLowerCase();
      return (
        (p.supplierName ?? '').toLowerCase().includes(term) ||
        String(p.purchaseNumber ?? '').includes(term) ||
        statusLabel.includes(term)
      );
    });
  });

  readonly stats = computed(() => {
    const list = this.purchases();
    return {
      total: list.length,
      confirmed: list.filter((p) => p.status === 'CONFIRMED').length,
      draft: list.filter((p) => p.status === 'DRAFT').length,
      amount: list
        .filter((p) => p.status === 'CONFIRMED')
        .reduce((acc, p) => acc + Number(p.total ?? 0), 0),
    };
  });

  constructor() {
    this.searchControl.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((value) => this.searchTerm.set(value));
  }

  ngOnInit(): void {
    this.loadPurchases();
  }

  loadPurchases(): void {
    this.loading.set(true);
    this.purchaseService.list().subscribe({
      next: (purchases) => {
        this.purchases.set(purchases);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('No se pudieron cargar las compras', 'Cerrar', { duration: 4000 });
      },
    });
  }

  statusBadgeClass(status: PurchaseStatus): string {
    switch (status) {
      case 'CONFIRMED':
        return 'bg-emerald-100 text-emerald-700';
      case 'CANCELLED':
        return 'bg-rose-100 text-rose-700';
      default:
        return 'bg-amber-100 text-amber-700';
    }
  }

  openCreate(): void {
    const ref = this.dialog.open(PurchaseFormDialogComponent, {
      autoFocus: false,
      width: '820px',
      maxWidth: '96vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.loadPurchases();
      }
    });
  }

  openDetail(purchase: PurchaseListItem): void {
    const ref = this.dialog.open(PurchaseDetailDialogComponent, {
      data: { purchaseId: purchase.id } as PurchaseDetailDialogData,
      autoFocus: false,
      width: '760px',
      maxWidth: '96vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((changed) => {
      if (changed) {
        this.loadPurchases();
      }
    });
  }

  confirmPurchase(purchase: PurchaseListItem): void {
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
      this.purchaseService.confirm(purchase.id).subscribe({
        next: () => {
          this.snackBar.open('Compra confirmada. Inventario actualizado.', 'Cerrar', { duration: 3500 });
          this.loadPurchases();
        },
        error: (error) => {
          const message = error?.error?.message ?? 'No se pudo confirmar la compra';
          this.snackBar.open(message, 'Cerrar', { duration: 4500 });
        },
      });
    });
  }

  cancelPurchase(purchase: PurchaseListItem): void {
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
      this.purchaseService.cancel(purchase.id).subscribe({
        next: () => {
          this.snackBar.open('Compra cancelada correctamente', 'Cerrar', { duration: 3500 });
          this.loadPurchases();
        },
        error: (error) => {
          const message =
            error?.error?.message ??
            'No se pudo cancelar la compra porque algunos productos ya no tienen stock suficiente para revertir el ingreso.';
          this.snackBar.open(message, 'Cerrar', { duration: 5000 });
        },
      });
    });
  }
}
