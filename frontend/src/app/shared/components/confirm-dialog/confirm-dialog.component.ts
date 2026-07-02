import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  danger?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  template: `
    <div class="w-full p-6">
      <div class="mb-4 flex items-start gap-3">
        <div
          class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl"
          [class]="data.danger ? 'bg-red-100 text-red-600' : 'bg-blue-100 text-blue-600'"
        >
          <span class="material-icons">{{ data.danger ? 'warning' : 'help_outline' }}</span>
        </div>
        <div>
          <h2 class="text-base font-semibold text-slate-800">{{ data.title }}</h2>
          <p class="mt-1 text-sm text-slate-500">{{ data.message }}</p>
        </div>
      </div>

      <div class="mt-6 flex justify-end gap-3">
        <button
          type="button"
          (click)="close(false)"
          class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
        >
          {{ data.cancelText ?? 'Cancelar' }}
        </button>
        <button
          type="button"
          (click)="close(true)"
          class="rounded-xl px-4 py-2 text-sm font-medium text-white transition focus:outline-none focus:ring-2 focus:ring-offset-2"
          [class]="
            data.danger
              ? 'bg-red-600 hover:bg-red-700 focus:ring-red-500'
              : 'bg-blue-600 hover:bg-blue-700 focus:ring-blue-500'
          "
        >
          {{ data.confirmText ?? 'Confirmar' }}
        </button>
      </div>
    </div>
  `,
})
export class ConfirmDialogComponent {
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ConfirmDialogComponent>);

  close(result: boolean): void {
    this.dialogRef.close(result);
  }
}
