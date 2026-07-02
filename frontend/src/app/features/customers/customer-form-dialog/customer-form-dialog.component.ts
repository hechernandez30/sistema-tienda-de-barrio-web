import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DatePipe } from '@angular/common';

import { CustomerService } from '../../../core/services/customer.service';
import { CustomerDetail, CustomerRequest } from '../../../core/models/customer.model';

export type CustomerDialogMode = 'create' | 'edit' | 'view';

export interface CustomerDialogData {
  mode: CustomerDialogMode;
  customer?: CustomerDetail;
}

@Component({
  selector: 'app-customer-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './customer-form-dialog.component.html',
})
export class CustomerFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly customerService = inject(CustomerService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<CustomerFormDialogComponent>);
  readonly data = inject<CustomerDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly isView = this.data.mode === 'view';
  readonly customer = this.data.customer ?? null;

  readonly title = computed(() => {
    switch (this.data.mode) {
      case 'create':
        return 'Nuevo cliente';
      case 'edit':
        return 'Editar cliente';
      default:
        return 'Detalle del cliente';
    }
  });

  readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(160)]],
    nit: ['', [Validators.maxLength(30)]],
    phone: ['', [Validators.pattern(/^\d{8}$/)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    address: [''],
    isActive: [true],
  });

  constructor() {
    if (this.customer) {
      this.form.patchValue({
        fullName: this.customer.fullName,
        nit: this.customer.nit ?? '',
        phone: this.customer.phone ?? '',
        email: this.customer.email ?? '',
        address: this.customer.address ?? '',
        isActive: this.customer.active,
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
        ? this.customerService.create(payload)
        : this.customerService.update(this.customer!.id, payload);

    request$.subscribe({
      next: (result) => {
        this.saving.set(false);
        this.snackBar.open(
          this.data.mode === 'create' ? 'Cliente creado correctamente' : 'Cliente actualizado correctamente',
          'Cerrar',
          { duration: 3000 },
        );
        // Devuelve el cliente creado/actualizado. La lista de Clientes solo verifica
        // que el valor sea "verdadero", por lo que sigue funcionando igual.
        this.dialogRef.close(result);
      },
      error: (error) => {
        this.saving.set(false);
        const message = error?.error?.message ?? 'No se pudo guardar el cliente';
        this.snackBar.open(message, 'Cerrar', { duration: 4000 });
      },
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  /** Sólo permite dígitos y limita el teléfono a 8 caracteres (elimina letras, "e" y pegados inválidos). */
  onPhoneInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 8);
    if (digits !== input.value) {
      input.value = digits;
    }
    this.form.controls.phone.setValue(digits);
  }

  private buildPayload(): CustomerRequest {
    const raw = this.form.getRawValue();
    return {
      fullName: raw.fullName.trim(),
      nit: this.emptyToNull(raw.nit),
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
