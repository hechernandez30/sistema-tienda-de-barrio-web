import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DatePipe } from '@angular/common';

import { SupplierService } from '../../../core/services/supplier.service';
import { SupplierDetail, SupplierRequest } from '../../../core/models/supplier.model';

export type SupplierDialogMode = 'create' | 'edit' | 'view';

export interface SupplierDialogData {
  mode: SupplierDialogMode;
  supplier?: SupplierDetail;
}

@Component({
  selector: 'app-supplier-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './supplier-form-dialog.component.html',
})
export class SupplierFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly supplierService = inject(SupplierService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<SupplierFormDialogComponent>);
  readonly data = inject<SupplierDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly isView = this.data.mode === 'view';
  readonly supplier = this.data.supplier ?? null;

  readonly title = computed(() => {
    switch (this.data.mode) {
      case 'create':
        return 'Nuevo proveedor';
      case 'edit':
        return 'Editar proveedor';
      default:
        return 'Detalle del proveedor';
    }
  });

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    nit: ['', [Validators.maxLength(30)]],
    contactName: ['', [Validators.maxLength(120)]],
    phone: ['', [Validators.maxLength(30)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    address: [''],
    isActive: [true],
  });

  constructor() {
    if (this.supplier) {
      this.form.patchValue({
        name: this.supplier.name,
        nit: this.supplier.nit ?? '',
        contactName: this.supplier.contactName ?? '',
        phone: this.supplier.phone ?? '',
        email: this.supplier.email ?? '',
        address: this.supplier.address ?? '',
        isActive: this.supplier.active,
      });
    }
    if (this.isView) {
      this.form.disable();
    }
  }

  save(): void {
    if (this.isView) {
      this.dialogRef.close(false);
      return;
    }
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const payload = this.buildPayload();
    const request$ =
      this.data.mode === 'create'
        ? this.supplierService.create(payload)
        : this.supplierService.update(this.supplier!.id, payload);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open(
          this.data.mode === 'create' ? 'Proveedor creado correctamente' : 'Proveedor actualizado correctamente',
          'Cerrar',
          { duration: 3000 },
        );
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.saving.set(false);
        const message = error?.error?.message ?? 'No se pudo guardar el proveedor';
        this.snackBar.open(message, 'Cerrar', { duration: 4000 });
      },
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  private buildPayload(): SupplierRequest {
    const raw = this.form.getRawValue();
    return {
      name: raw.name.trim(),
      nit: this.emptyToNull(raw.nit),
      contactName: this.emptyToNull(raw.contactName),
      phone: this.emptyToNull(raw.phone),
      email: this.emptyToNull(raw.email),
      address: this.emptyToNull(raw.address),
      isActive: raw.isActive,
    };
  }

  private emptyToNull(value: string): string | null {
    const trimmed = value?.trim();
    return trimmed ? trimmed : null;
  }
}
