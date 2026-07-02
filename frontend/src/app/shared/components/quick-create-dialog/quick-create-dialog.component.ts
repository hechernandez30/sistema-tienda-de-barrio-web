import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface QuickCreateField {
  key: string;
  label: string;
  placeholder?: string;
  required?: boolean;
  maxLength?: number;
  /** Convierte el valor a mayúsculas (útil para códigos). */
  uppercase?: boolean;
}

export interface QuickCreateDialogData {
  title: string;
  subtitle?: string;
  icon?: string;
  confirmText?: string;
  fields: QuickCreateField[];
}

/**
 * Diálogo genérico para crear rápidamente un registro sencillo (por ejemplo, una
 * categoría o unidad de medida). Devuelve un objeto con los valores capturados, o
 * `undefined` si se cancela. No llama a ningún servicio: eso lo hace quien lo abre.
 */
@Component({
  selector: 'app-quick-create-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="w-full p-6">
      <div class="mb-5 flex items-start gap-3">
        <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-blue-100 text-blue-600">
          <span class="material-icons">{{ data.icon ?? 'add' }}</span>
        </div>
        <div>
          <h2 class="text-base font-semibold text-slate-800">{{ data.title }}</h2>
          @if (data.subtitle) {
            <p class="mt-1 text-sm text-slate-500">{{ data.subtitle }}</p>
          }
        </div>
      </div>

      <form [formGroup]="form" (ngSubmit)="save()" class="flex flex-col gap-4">
        @for (field of data.fields; track field.key) {
          <div>
            <label class="mb-1.5 block text-sm font-medium text-slate-700">
              {{ field.label }}@if (field.required) { <span>*</span> }
            </label>
            <input
              type="text"
              [formControlName]="field.key"
              [placeholder]="field.placeholder ?? ''"
              [attr.maxlength]="field.maxLength ?? null"
              [class.uppercase]="field.uppercase"
              class="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500"
              [class.border-red-400]="form.get(field.key)?.touched && form.get(field.key)?.invalid"
            />
            @if (form.get(field.key)?.touched && form.get(field.key)?.hasError('required')) {
              <p class="mt-1 text-xs text-red-600">Este campo es obligatorio</p>
            }
          </div>
        }

        <div class="mt-2 flex justify-end gap-3">
          <button
            type="button"
            (click)="cancel()"
            class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
          >
            Cancelar
          </button>
          <button
            type="submit"
            class="rounded-xl bg-blue-600 px-5 py-2 text-sm font-medium text-white transition hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            {{ data.confirmText ?? 'Crear' }}
          </button>
        </div>
      </form>
    </div>
  `,
})
export class QuickCreateDialogComponent {
  readonly data = inject<QuickCreateDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<QuickCreateDialogComponent>);

  readonly form: FormGroup;

  constructor() {
    const controls: Record<string, FormControl<string>> = {};
    for (const field of this.data.fields) {
      controls[field.key] = new FormControl('', {
        nonNullable: true,
        validators: field.required ? [Validators.required] : [],
      });
    }
    this.form = new FormGroup(controls);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue() as Record<string, string>;
    const result: Record<string, string> = {};
    for (const field of this.data.fields) {
      const value = (raw[field.key] ?? '').trim();
      result[field.key] = field.uppercase ? value.toUpperCase() : value;
    }
    this.dialogRef.close(result);
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }
}
