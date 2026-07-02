import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

import { SupplierService } from '../../core/services/supplier.service';
import { SupplierDetail, SupplierListItem } from '../../core/models/supplier.model';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../shared/components/confirm-dialog/confirm-dialog.component';
import {
  SupplierDialogData,
  SupplierDialogMode,
  SupplierFormDialogComponent,
} from './supplier-form-dialog/supplier-form-dialog.component';

@Component({
  selector: 'app-suppliers',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './suppliers.component.html',
})
export class SuppliersComponent implements OnInit {
  private readonly supplierService = inject(SupplierService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly suppliers = signal<SupplierListItem[]>([]);
  readonly loading = signal(false);
  readonly hasSearchTerm = signal(false);
  readonly searchControl = new FormControl('', { nonNullable: true });

  readonly stats = computed(() => {
    const list = this.suppliers();
    return {
      total: list.length,
      active: list.filter((s) => s.active).length,
      inactive: list.filter((s) => !s.active).length,
      withNit: list.filter((s) => !!s.nit && s.nit.trim().length > 0).length,
    };
  });

  constructor() {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((term) => {
          const clean = term.trim();
          this.hasSearchTerm.set(clean.length > 0);
          this.loading.set(true);
          return this.fetch(clean);
        }),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (list) => {
          this.suppliers.set(list);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.snackBar.open('No se pudieron cargar los proveedores', 'Cerrar', { duration: 4000 });
        },
      });
  }

  ngOnInit(): void {
    this.loadSuppliers();
  }

  loadSuppliers(): void {
    const clean = this.searchControl.value.trim();
    this.hasSearchTerm.set(clean.length > 0);
    this.loading.set(true);
    this.fetch(clean).subscribe({
      next: (list) => {
        this.suppliers.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('No se pudieron cargar los proveedores', 'Cerrar', { duration: 4000 });
      },
    });
  }

  openCreate(): void {
    this.openDialog({ mode: 'create' });
  }

  openEdit(supplier: SupplierListItem): void {
    this.openWithDetail(supplier.id, 'edit');
  }

  openView(supplier: SupplierListItem): void {
    this.openWithDetail(supplier.id, 'view');
  }

  confirmDelete(supplier: SupplierListItem): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Eliminar proveedor',
        message: `¿Deseas eliminar "${supplier.name}"? Esta acción realiza un borrado lógico.`,
        confirmText: 'Eliminar',
        danger: true,
      } as ConfirmDialogData,
      autoFocus: false,
      width: '26rem',
      maxWidth: '92vw',
      panelClass: 'app-dialog',
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.supplierService.delete(supplier.id).subscribe({
        next: () => {
          this.snackBar.open('Proveedor eliminado correctamente', 'Cerrar', { duration: 3000 });
          this.loadSuppliers();
        },
        error: (error) => {
          const message = error?.error?.message ?? 'No se pudo eliminar el proveedor';
          this.snackBar.open(message, 'Cerrar', { duration: 4000 });
        },
      });
    });
  }

  private fetch(term: string): Observable<SupplierListItem[]> {
    return term ? this.supplierService.search(term) : this.supplierService.list();
  }

  private openWithDetail(id: string, mode: 'edit' | 'view'): void {
    this.supplierService.getById(id).subscribe({
      next: (supplier: SupplierDetail) => this.openDialog({ mode, supplier }),
      error: () => this.snackBar.open('No se pudo cargar el proveedor', 'Cerrar', { duration: 4000 }),
    });
  }

  private openDialog(data: { mode: SupplierDialogMode; supplier?: SupplierDetail }): void {
    const ref = this.dialog.open(SupplierFormDialogComponent, {
      data: data as SupplierDialogData,
      autoFocus: false,
      width: '640px',
      maxWidth: '94vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.loadSuppliers();
      }
    });
  }
}
