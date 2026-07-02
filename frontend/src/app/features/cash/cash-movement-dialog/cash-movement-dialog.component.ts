import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CashService } from '../../../core/services/cash.service';
import {
  CashMovementType,
  EXPENSE_CATEGORIES,
  INCOME_CATEGORIES,
  PaymentMethod,
} from '../../../core/models/cash.model';

@Component({
  selector: 'app-cash-movement-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './cash-movement-dialog.component.html',
})
export class CashMovementDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly cashService = inject(CashService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<CashMovementDialogComponent>);

  readonly saving = signal(false);

  readonly paymentMethods: { value: PaymentMethod; label: string }[] = [
    { value: 'CASH', label: 'Efectivo' },
    { value: 'TRANSFER', label: 'Transferencia' },
    { value: 'CARD', label: 'Tarjeta' },
  ];

  readonly form = this.fb.nonNullable.group({
    movementType: ['INCOME' as CashMovementType, [Validators.required]],
    category: ['', [Validators.required]],
    paymentMethod: ['CASH' as PaymentMethod, [Validators.required]],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    description: [''],
  });

  private readonly movementType = toSignal(this.form.controls.movementType.valueChanges, {
    initialValue: this.form.controls.movementType.value,
  });

  readonly categories = computed(() =>
    this.movementType() === 'INCOME' ? INCOME_CATEGORIES : EXPENSE_CATEGORIES,
  );

  constructor() {
    // Al cambiar el tipo, reiniciar la categoría para evitar valores inválidos.
    this.form.controls.movementType.valueChanges.subscribe(() => {
      this.form.controls.category.setValue('');
    });
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.cashService
      .registerMovement({
        movementType: raw.movementType,
        category: raw.category,
        paymentMethod: raw.paymentMethod,
        amount: Number(raw.amount),
        description: raw.description?.trim() ? raw.description.trim() : null,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.snackBar.open('Movimiento registrado correctamente', 'Cerrar', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: (error) => {
          this.saving.set(false);
          const message = error?.error?.message ?? 'No se pudo registrar el movimiento';
          this.snackBar.open(message, 'Cerrar', { duration: 4500 });
        },
      });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
