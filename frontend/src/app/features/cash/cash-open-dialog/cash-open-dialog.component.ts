import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CashService } from '../../../core/services/cash.service';

@Component({
  selector: 'app-cash-open-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './cash-open-dialog.component.html',
})
export class CashOpenDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly cashService = inject(CashService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<CashOpenDialogComponent>);

  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    openingAmount: [0, [Validators.required, Validators.min(0)]],
    notes: [''],
  });

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.cashService
      .openSession({ openingAmount: Number(raw.openingAmount), notes: raw.notes?.trim() ? raw.notes.trim() : null })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.snackBar.open('Caja abierta correctamente', 'Cerrar', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: (error) => {
          this.saving.set(false);
          const message = error?.error?.message ?? 'No se pudo abrir la caja';
          this.snackBar.open(message, 'Cerrar', { duration: 4500 });
        },
      });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
