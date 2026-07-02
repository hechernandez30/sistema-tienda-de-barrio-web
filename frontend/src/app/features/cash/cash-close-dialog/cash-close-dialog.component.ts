import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CashService } from '../../../core/services/cash.service';

export interface CashCloseDialogData {
  sessionId: string;
  expectedAmount: number;
}

@Component({
  selector: 'app-cash-close-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './cash-close-dialog.component.html',
})
export class CashCloseDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly cashService = inject(CashService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<CashCloseDialogComponent>);
  readonly data = inject<CashCloseDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    countedAmount: [0, [Validators.required, Validators.min(0)]],
    notes: [''],
  });

  private readonly countedAmount = toSignal(this.form.controls.countedAmount.valueChanges, {
    initialValue: this.form.controls.countedAmount.value,
  });

  readonly difference = computed(() => Number(this.countedAmount() ?? 0) - this.data.expectedAmount);

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.cashService
      .closeSession(this.data.sessionId, {
        countedAmount: Number(raw.countedAmount),
        notes: raw.notes?.trim() ? raw.notes.trim() : null,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.snackBar.open('Caja cerrada correctamente', 'Cerrar', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: (error) => {
          this.saving.set(false);
          const message = error?.error?.message ?? 'No se pudo cerrar la caja';
          this.snackBar.open(message, 'Cerrar', { duration: 4500 });
        },
      });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
