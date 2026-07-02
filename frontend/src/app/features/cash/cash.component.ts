import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../core/services/auth.service';
import { CashService } from '../../core/services/cash.service';
import {
  CASH_MOVEMENT_TYPE_LABELS,
  CASH_SESSION_STATUS_LABELS,
  CashMovement,
  CashSession,
  CurrentCashSummary,
  PAYMENT_METHOD_LABELS,
} from '../../core/models/cash.model';
import { CashOpenDialogComponent } from './cash-open-dialog/cash-open-dialog.component';
import {
  CashCloseDialogComponent,
  CashCloseDialogData,
} from './cash-close-dialog/cash-close-dialog.component';
import { CashMovementDialogComponent } from './cash-movement-dialog/cash-movement-dialog.component';
import {
  CashSessionDetailDialogComponent,
  CashSessionDetailDialogData,
} from './cash-session-detail-dialog/cash-session-detail-dialog.component';

type CashTab = 'current' | 'movements' | 'sessions';

@Component({
  selector: 'app-cash',
  standalone: true,
  imports: [DecimalPipe, DatePipe],
  templateUrl: './cash.component.html',
})
export class CashComponent implements OnInit {
  private readonly cashService = inject(CashService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly typeLabels = CASH_MOVEMENT_TYPE_LABELS;
  readonly paymentLabels = PAYMENT_METHOD_LABELS;
  readonly statusLabels = CASH_SESSION_STATUS_LABELS;

  readonly activeTab = signal<CashTab>('current');
  readonly summary = signal<CurrentCashSummary | null>(null);
  readonly movements = signal<CashMovement[]>([]);
  readonly sessions = signal<CashSession[]>([]);

  readonly loadingCurrent = signal(false);
  readonly loadingMovements = signal(false);
  readonly loadingSessions = signal(false);

  // Gating por rol alineado al backend:
  //  - Operar caja (abrir/cerrar/movimiento): ADMIN, CAJERO
  //  - Historial (listar movimientos y sesiones): ADMIN, REPORTES
  readonly canOperate = computed(() => this.authService.hasAnyRole(['ADMIN', 'CAJERO']));
  readonly canViewHistory = computed(() => this.authService.hasAnyRole(['ADMIN', 'REPORTES']));

  readonly hasOpenSession = computed(() => !!this.summary()?.cashSessionId);

  ngOnInit(): void {
    this.loadCurrent();
    if (this.canViewHistory()) {
      this.loadSessions();
    } else {
      // CAJERO no puede consultar el historial global: se queda en la pestaña de caja actual.
      this.activeTab.set('current');
    }
  }

  setTab(tab: CashTab): void {
    this.activeTab.set(tab);
    if (tab === 'movements' && this.movements().length === 0) {
      this.loadMovements();
    }
    if (tab === 'sessions' && this.sessions().length === 0) {
      this.loadSessions();
    }
  }

  loadCurrent(): void {
    this.loadingCurrent.set(true);
    this.cashService.getCurrentSession().subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loadingCurrent.set(false);
      },
      error: () => {
        this.loadingCurrent.set(false);
        this.snackBar.open('No se pudo consultar la caja actual', 'Cerrar', { duration: 4000 });
      },
    });
  }

  loadMovements(): void {
    this.loadingMovements.set(true);
    this.cashService.listMovements().subscribe({
      next: (list) => {
        this.movements.set(list);
        this.loadingMovements.set(false);
      },
      error: () => {
        this.loadingMovements.set(false);
        this.snackBar.open('No se pudieron cargar los movimientos', 'Cerrar', { duration: 4000 });
      },
    });
  }

  loadSessions(): void {
    this.loadingSessions.set(true);
    this.cashService.listSessions().subscribe({
      next: (list) => {
        this.sessions.set(list);
        this.loadingSessions.set(false);
      },
      error: () => {
        this.loadingSessions.set(false);
        this.snackBar.open('No se pudieron cargar las sesiones', 'Cerrar', { duration: 4000 });
      },
    });
  }

  openCashDialog(): void {
    const ref = this.dialog.open(CashOpenDialogComponent, {
      autoFocus: false,
      width: '460px',
      maxWidth: '92vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.refreshAll();
      }
    });
  }

  closeCashDialog(): void {
    const current = this.summary();
    if (!current?.cashSessionId) {
      return;
    }
    const ref = this.dialog.open(CashCloseDialogComponent, {
      data: {
        sessionId: current.cashSessionId,
        expectedAmount: Number(current.expectedAmount ?? 0),
      } as CashCloseDialogData,
      autoFocus: false,
      width: '460px',
      maxWidth: '92vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.refreshAll();
      }
    });
  }

  newMovementDialog(): void {
    if (!this.hasOpenSession()) {
      this.snackBar.open('Debes abrir caja antes de registrar movimientos.', 'Cerrar', { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(CashMovementDialogComponent, {
      autoFocus: false,
      width: '620px',
      maxWidth: '94vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.loadCurrent();
        if (this.canViewHistory()) {
          this.loadMovements();
        }
      }
    });
  }

  openSessionDetail(session: CashSession): void {
    this.dialog.open(CashSessionDetailDialogComponent, {
      data: { sessionId: session.id } as CashSessionDetailDialogData,
      autoFocus: false,
      width: '780px',
      maxWidth: '96vw',
      panelClass: 'app-dialog',
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

  private refreshAll(): void {
    this.loadCurrent();
    if (this.canViewHistory()) {
      this.loadSessions();
      this.loadMovements();
    }
  }
}
