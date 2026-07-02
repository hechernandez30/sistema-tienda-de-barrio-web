import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { UserService } from '../../../core/services/user.service';

export interface ChangePasswordDialogData {
  userId: string;
  username: string;
}

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;

@Component({
  selector: 'app-change-password-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './change-password-dialog.component.html',
})
export class ChangePasswordDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<ChangePasswordDialogComponent>);
  readonly data = inject<ChangePasswordDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: [passwordMatchValidator] },
  );

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.userService
      .changePassword(this.data.userId, { newPassword: this.form.controls.newPassword.value })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.snackBar.open('Contraseña actualizada correctamente', 'Cerrar', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: (error) => {
          this.saving.set(false);
          const message = error?.error?.message ?? 'No se pudo cambiar la contraseña';
          this.snackBar.open(message, 'Cerrar', { duration: 4500 });
        },
      });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  hasError(control: string, error: string): boolean {
    const ctrl = this.form.get(control);
    return !!ctrl && ctrl.touched && ctrl.hasError(error);
  }

  get passwordsMismatch(): boolean {
    return this.form.hasError('passwordMismatch') && !!this.form.controls.confirmPassword.touched;
  }
}

function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  if (!password || !confirm) {
    return null;
  }
  return password === confirm ? null : { passwordMismatch: true };
}
