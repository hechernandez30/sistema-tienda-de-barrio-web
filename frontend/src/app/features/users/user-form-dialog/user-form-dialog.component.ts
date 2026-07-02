import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
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
import { RoleService } from '../../../core/services/role.service';
import {
  Role,
  UserCreateRequest,
  UserDetail,
  UserUpdateRequest,
} from '../../../core/models/user.model';

export type UserDialogMode = 'create' | 'edit' | 'view';

export interface UserDialogData {
  mode: UserDialogMode;
  user?: UserDetail;
}

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;

@Component({
  selector: 'app-user-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './user-form-dialog.component.html',
})
export class UserFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly roleService = inject(RoleService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<UserFormDialogComponent>);
  readonly data = inject<UserDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly roles = signal<Role[]>([]);
  readonly loadingRoles = signal(true);

  readonly isCreate = this.data.mode === 'create';
  readonly isEdit = this.data.mode === 'edit';
  readonly isView = this.data.mode === 'view';
  readonly user = this.data.user ?? null;

  readonly title = computed(() => {
    switch (this.data.mode) {
      case 'create':
        return 'Nuevo usuario';
      case 'edit':
        return 'Editar usuario';
      default:
        return 'Detalle del usuario';
    }
  });

  readonly form = this.fb.nonNullable.group(
    {
      username: ['', [Validators.required, Validators.maxLength(50)]],
      email: ['', [Validators.email, Validators.maxLength(120)]],
      password: [''],
      confirmPassword: [''],
      firstName: ['', [Validators.required, Validators.maxLength(80)]],
      lastName: ['', [Validators.required, Validators.maxLength(80)]],
      roleId: ['', [Validators.required]],
      active: [true],
    },
    { validators: this.isCreate ? [passwordMatchValidator] : [] },
  );

  constructor() {
    if (this.isCreate) {
      this.form.controls.password.addValidators([Validators.required, Validators.pattern(PASSWORD_PATTERN)]);
      this.form.controls.confirmPassword.addValidators([Validators.required]);
    }

    if (this.user) {
      this.form.patchValue({
        username: this.user.username,
        email: this.user.email ?? '',
        firstName: this.user.firstName,
        lastName: this.user.lastName,
        roleId: this.user.role?.id ?? '',
        active: this.user.active,
      });
    }

    if (this.isView) {
      this.form.disable();
    }

    this.roleService.list().subscribe({
      next: (roles) => {
        // Solo roles activos; conserva el rol actual del usuario aunque esté inactivo.
        const activeRoles = roles.filter((r) => r.active || r.id === this.user?.role?.id);
        this.roles.set(activeRoles);
        this.loadingRoles.set(false);
      },
      error: () => {
        this.loadingRoles.set(false);
        this.snackBar.open('No se pudieron cargar los roles', 'Cerrar', { duration: 4000 });
      },
    });
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
    const raw = this.form.getRawValue();

    const request$ = this.isCreate
      ? this.userService.create({
          roleId: raw.roleId,
          username: raw.username.trim(),
          email: this.emptyToNull(raw.email),
          password: raw.password,
          firstName: raw.firstName.trim(),
          lastName: raw.lastName.trim(),
          active: raw.active,
        } as UserCreateRequest)
      : this.userService.update(this.user!.id, {
          roleId: raw.roleId,
          email: this.emptyToNull(raw.email),
          firstName: raw.firstName.trim(),
          lastName: raw.lastName.trim(),
          active: raw.active,
        } as UserUpdateRequest);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open(
          this.isCreate ? 'Usuario creado correctamente' : 'Usuario actualizado correctamente',
          'Cerrar',
          { duration: 3000 },
        );
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.saving.set(false);
        const message = error?.error?.message ?? 'No se pudo guardar el usuario';
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
    return (
      this.form.hasError('passwordMismatch') &&
      !!this.form.controls.confirmPassword.touched
    );
  }

  private emptyToNull(value: string): string | null {
    const trimmed = value?.trim();
    return trimmed ? trimmed : null;
  }
}

function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirmPassword')?.value;
  if (!password || !confirm) {
    return null;
  }
  return password === confirm ? null : { passwordMismatch: true };
}
