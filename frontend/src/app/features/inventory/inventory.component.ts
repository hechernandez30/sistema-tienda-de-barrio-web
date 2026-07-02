import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { InventoryService } from '../../core/services/inventory.service';
import {
  INCOMING_MOVEMENT_TYPES,
  InventoryMovement,
  InventoryMovementType,
  LowStockProduct,
  MOVEMENT_TYPE_LABELS,
  OUTGOING_MOVEMENT_TYPES,
} from '../../core/models/inventory.model';
import {
  AdjustmentDialogData,
  AdjustmentMode,
  InventoryAdjustmentDialogComponent,
  SelectedProduct,
} from './inventory-adjustment-dialog/inventory-adjustment-dialog.component';

type InventoryTab = 'movements' | 'lowStock';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe, DatePipe],
  templateUrl: './inventory.component.html',
})
export class InventoryComponent implements OnInit {
  private readonly inventoryService = inject(InventoryService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly movementLabels = MOVEMENT_TYPE_LABELS;
  readonly movementTypes = Object.keys(MOVEMENT_TYPE_LABELS) as InventoryMovementType[];

  readonly activeTab = signal<InventoryTab>('movements');
  readonly movements = signal<InventoryMovement[]>([]);
  readonly lowStock = signal<LowStockProduct[]>([]);
  readonly loadingMovements = signal(false);
  readonly loadingLowStock = signal(false);

  readonly searchTerm = signal('');
  readonly typeFilter = signal<InventoryMovementType | ''>('');
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly typeControl = new FormControl<InventoryMovementType | ''>('', { nonNullable: true });

  readonly filteredMovements = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const type = this.typeFilter();
    return this.movements().filter((m) => {
      const matchesType = !type || m.movementType === type;
      const matchesTerm =
        !term ||
        [m.productName, m.barcode]
          .filter((v): v is string => !!v)
          .some((v) => v.toLowerCase().includes(term));
      return matchesType && matchesTerm;
    });
  });

  readonly stats = computed(() => {
    const list = this.movements();
    const today = new Date().toDateString();
    const isToday = (iso: string) => {
      const date = new Date(iso);
      return !Number.isNaN(date.getTime()) && date.toDateString() === today;
    };
    return {
      totalMovements: list.length,
      lowStockCount: this.lowStock().length,
      incomingToday: list.filter((m) => isToday(m.createdAt) && INCOMING_MOVEMENT_TYPES.includes(m.movementType)).length,
      outgoingToday: list.filter((m) => isToday(m.createdAt) && OUTGOING_MOVEMENT_TYPES.includes(m.movementType)).length,
    };
  });

  constructor() {
    this.searchControl.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((value) => this.searchTerm.set(value));
    this.typeControl.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => this.typeFilter.set(value));
  }

  ngOnInit(): void {
    this.loadMovements();
    this.loadLowStock();
  }

  setTab(tab: InventoryTab): void {
    this.activeTab.set(tab);
  }

  isIncoming(type: InventoryMovementType): boolean {
    return INCOMING_MOVEMENT_TYPES.includes(type);
  }

  badgeClass(type: InventoryMovementType): string {
    switch (type) {
      case 'PURCHASE':
        return 'bg-blue-100 text-blue-700';
      case 'SALE':
        return 'bg-violet-100 text-violet-700';
      case 'ADJUSTMENT_IN':
        return 'bg-emerald-100 text-emerald-700';
      case 'ADJUSTMENT_OUT':
        return 'bg-amber-100 text-amber-700';
      case 'SALE_CANCEL':
        return 'bg-rose-100 text-rose-700';
      case 'PURCHASE_CANCEL':
        return 'bg-slate-100 text-slate-600';
      default:
        return 'bg-slate-100 text-slate-600';
    }
  }

  loadMovements(): void {
    this.loadingMovements.set(true);
    this.inventoryService.listMovements().subscribe({
      next: (movements) => {
        this.movements.set(movements);
        this.loadingMovements.set(false);
      },
      error: () => {
        this.loadingMovements.set(false);
        this.snackBar.open('No se pudieron cargar los movimientos', 'Cerrar', { duration: 4000 });
      },
    });
  }

  loadLowStock(): void {
    this.loadingLowStock.set(true);
    this.inventoryService.lowStock().subscribe({
      next: (items) => {
        this.lowStock.set(items);
        this.loadingLowStock.set(false);
      },
      error: () => {
        this.loadingLowStock.set(false);
        this.snackBar.open('No se pudo cargar el stock bajo', 'Cerrar', { duration: 4000 });
      },
    });
  }

  openAdjustment(mode: AdjustmentMode, product?: SelectedProduct): void {
    const ref = this.dialog.open(InventoryAdjustmentDialogComponent, {
      data: { mode, product } as AdjustmentDialogData,
      autoFocus: false,
      width: '560px',
      maxWidth: '94vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.loadMovements();
        this.loadLowStock();
      }
    });
  }

  adjustFromLowStock(item: LowStockProduct): void {
    this.openAdjustment('in', {
      id: item.productId,
      name: item.name,
      barcode: item.barcode,
      currentStock: item.currentStock,
    });
  }
}
