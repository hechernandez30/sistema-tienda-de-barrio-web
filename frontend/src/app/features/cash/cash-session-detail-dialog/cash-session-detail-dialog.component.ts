import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';

import { CashService } from '../../../core/services/cash.service';
import {
  CASH_MOVEMENT_TYPE_LABELS,
  CASH_SESSION_STATUS_LABELS,
  CashMovement,
  CashSession,
  PAYMENT_METHOD_LABELS,
} from '../../../core/models/cash.model';

export interface CashSessionDetailDialogData {
  sessionId: string;
}

@Component({
  selector: 'app-cash-session-detail-dialog',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './cash-session-detail-dialog.component.html',
})
export class CashSessionDetailDialogComponent {
  private readonly cashService = inject(CashService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<CashSessionDetailDialogComponent>);
  private readonly data = inject<CashSessionDetailDialogData>(MAT_DIALOG_DATA);

  readonly session = signal<CashSession | null>(null);
  readonly movements = signal<CashMovement[]>([]);
  readonly loading = signal(true);

  readonly statusLabels = CASH_SESSION_STATUS_LABELS;
  readonly typeLabels = CASH_MOVEMENT_TYPE_LABELS;
  readonly paymentLabels = PAYMENT_METHOD_LABELS;

  constructor() {
    forkJoin({
      session: this.cashService.getSession(this.data.sessionId),
      movements: this.cashService.getSessionMovements(this.data.sessionId),
    }).subscribe({
      next: ({ session, movements }) => {
        this.session.set(session);
        this.movements.set(movements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('No se pudo cargar la sesión', 'Cerrar', { duration: 4000 });
      },
    });
  }

  referenceLabel(movement: CashMovement): string {
    if (movement.referenceSaleId) {
      return 'Venta';
    }
    if (movement.referencePurchaseId) {
      return 'Compra';
    }
    return 'Manual';
  }

  differenceLabel(session: CashSession): string {
    if (session.status === 'OPEN' || session.differenceAmount === null || session.differenceAmount === undefined) {
      return 'Pendiente';
    }
    const diff = Number(session.differenceAmount);
    if (diff === 0) {
      return 'Cuadrada';
    }
    return diff > 0 ? 'Sobrante' : 'Faltante';
  }

  close(): void {
    this.dialogRef.close();
  }
}
