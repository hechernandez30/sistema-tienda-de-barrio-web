import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

import { CustomerService } from '../../core/services/customer.service';
import { CustomerDetail, CustomerListItem } from '../../core/models/customer.model';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../shared/components/confirm-dialog/confirm-dialog.component';
import {
  CustomerDialogData,
  CustomerDialogMode,
  CustomerFormDialogComponent,
} from './customer-form-dialog/customer-form-dialog.component';

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './customers.component.html',
})
export class CustomersComponent implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly customers = signal<CustomerListItem[]>([]);
  readonly loading = signal(false);
  readonly hasSearchTerm = signal(false);
  readonly searchControl = new FormControl('', { nonNullable: true });

  readonly stats = computed(() => {
    const list = this.customers();
    return {
      total: list.length,
      active: list.filter((c) => c.active).length,
      inactive: list.filter((c) => !c.active).length,
      withNit: list.filter((c) => !!c.nit && c.nit.trim().length > 0).length,
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
          this.customers.set(list);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.snackBar.open('No se pudieron cargar los clientes', 'Cerrar', { duration: 4000 });
        },
      });
  }

  ngOnInit(): void {
    this.loadCustomers();
  }

  loadCustomers(): void {
    const clean = this.searchControl.value.trim();
    this.hasSearchTerm.set(clean.length > 0);
    this.loading.set(true);
    this.fetch(clean).subscribe({
      next: (list) => {
        this.customers.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('No se pudieron cargar los clientes', 'Cerrar', { duration: 4000 });
      },
    });
  }

  openCreate(): void {
    this.openDialog({ mode: 'create' });
  }

  openEdit(customer: CustomerListItem): void {
    this.openWithDetail(customer.id, 'edit');
  }

  openView(customer: CustomerListItem): void {
    this.openWithDetail(customer.id, 'view');
  }

  confirmDelete(customer: CustomerListItem): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Eliminar cliente',
        message: `¿Deseas eliminar a "${customer.fullName}"? Esta acción realiza un borrado lógico.`,
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
      this.customerService.delete(customer.id).subscribe({
        next: () => {
          this.snackBar.open('Cliente eliminado correctamente', 'Cerrar', { duration: 3000 });
          this.loadCustomers();
        },
        error: (error) => {
          const message = error?.error?.message ?? 'No se pudo eliminar el cliente';
          this.snackBar.open(message, 'Cerrar', { duration: 4000 });
        },
      });
    });
  }

  private fetch(term: string): Observable<CustomerListItem[]> {
    return term ? this.customerService.search(term) : this.customerService.list();
  }

  private openWithDetail(id: string, mode: 'edit' | 'view'): void {
    this.customerService.getById(id).subscribe({
      next: (customer: CustomerDetail) => this.openDialog({ mode, customer }),
      error: () => this.snackBar.open('No se pudo cargar el cliente', 'Cerrar', { duration: 4000 }),
    });
  }

  private openDialog(data: { mode: CustomerDialogMode; customer?: CustomerDetail }): void {
    const ref = this.dialog.open(CustomerFormDialogComponent, {
      data: data as CustomerDialogData,
      autoFocus: false,
      width: '640px',
      maxWidth: '94vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.loadCustomers();
      }
    });
  }
}
