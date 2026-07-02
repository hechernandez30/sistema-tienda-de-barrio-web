import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { Role, ROLE_EXPLANATIONS } from '../../../core/models/user.model';

export interface RoleDetailDialogData {
  role: Role;
}

@Component({
  selector: 'app-role-detail-dialog',
  standalone: true,
  templateUrl: './role-detail-dialog.component.html',
})
export class RoleDetailDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<RoleDetailDialogComponent>);
  readonly data = inject<RoleDetailDialogData>(MAT_DIALOG_DATA);

  get role(): Role {
    return this.data.role;
  }

  get explanation(): string {
    return ROLE_EXPLANATIONS[this.role.name] ?? this.role.description ?? 'Rol del sistema.';
  }

  close(): void {
    this.dialogRef.close();
  }
}
