import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { RoleService } from '../../core/services/role.service';
import {
  Role,
  ROLE_DESCRIPTIONS,
  UserDetail,
  UserListItem,
} from '../../core/models/user.model';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../shared/components/confirm-dialog/confirm-dialog.component';
import {
  UserDialogData,
  UserDialogMode,
  UserFormDialogComponent,
} from './user-form-dialog/user-form-dialog.component';
import {
  ChangePasswordDialogComponent,
  ChangePasswordDialogData,
} from './change-password-dialog/change-password-dialog.component';
import {
  RoleDetailDialogComponent,
  RoleDetailDialogData,
} from './role-detail-dialog/role-detail-dialog.component';

type TabId = 'users' | 'roles';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './users.component.html',
})
export class UsersComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly roleService = inject(RoleService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly roleDescriptions = ROLE_DESCRIPTIONS;

  readonly activeTab = signal<TabId>('users');

  readonly users = signal<UserListItem[]>([]);
  readonly loadingUsers = signal(false);
  readonly searchControl = new FormControl('', { nonNullable: true });
  private readonly searchTerm = signal('');

  readonly roles = signal<Role[]>([]);
  readonly loadingRoles = signal(false);

  private readonly currentUserId = computed(() => this.authService.getCurrentUser()?.id ?? null);

  readonly filteredUsers = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const list = this.users();
    if (!term) {
      return list;
    }
    return list.filter((u) =>
      [u.username, u.firstName, u.lastName, u.email ?? '', u.roleName ?? '']
        .join(' ')
        .toLowerCase()
        .includes(term),
    );
  });

  readonly stats = computed(() => {
    const list = this.users();
    return {
      total: list.length,
      active: list.filter((u) => u.active).length,
      inactive: list.filter((u) => !u.active).length,
      admins: list.filter((u) => u.roleName === 'ADMIN').length,
    };
  });

  constructor() {
    this.searchControl.valueChanges.pipe(takeUntilDestroyed()).subscribe((value) => {
      this.searchTerm.set(value);
    });
  }

  ngOnInit(): void {
    this.loadUsers();
    this.loadRoles();
  }

  selectTab(tab: TabId): void {
    this.activeTab.set(tab);
  }

  isCurrentUser(user: UserListItem): boolean {
    return this.currentUserId() === user.id;
  }

  roleBadgeClass(roleName?: string | null): string {
    switch (roleName) {
      case 'ADMIN':
        return 'bg-purple-100 text-purple-700';
      case 'CAJERO':
        return 'bg-blue-100 text-blue-700';
      case 'INVENTARIO':
        return 'bg-amber-100 text-amber-700';
      case 'REPORTES':
        return 'bg-teal-100 text-teal-700';
      default:
        return 'bg-slate-100 text-slate-600';
    }
  }

  // ---------------------------------------------------------------
  // Usuarios
  // ---------------------------------------------------------------
  loadUsers(): void {
    this.loadingUsers.set(true);
    this.userService.list().subscribe({
      next: (list) => {
        this.users.set(list);
        this.loadingUsers.set(false);
      },
      error: () => {
        this.loadingUsers.set(false);
        this.snackBar.open('No se pudieron cargar los usuarios', 'Cerrar', { duration: 4000 });
      },
    });
  }

  openCreate(): void {
    this.openDialog({ mode: 'create' });
  }

  openEdit(user: UserListItem): void {
    this.openWithDetail(user.id, 'edit');
  }

  openView(user: UserListItem): void {
    this.openWithDetail(user.id, 'view');
  }

  openChangePassword(user: UserListItem): void {
    const ref = this.dialog.open(ChangePasswordDialogComponent, {
      data: { userId: user.id, username: user.username } as ChangePasswordDialogData,
      autoFocus: false,
      width: '30rem',
      maxWidth: '94vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe(() => {
      /* nada que recargar tras cambio de contraseña */
    });
  }

  activate(user: UserListItem): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Activar usuario',
        message: `¿Deseas activar a "${user.username}"? Podrá iniciar sesión nuevamente.`,
        confirmText: 'Activar',
      } as ConfirmDialogData,
      autoFocus: false,
      width: '26rem',
      maxWidth: '92vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((ok) => {
      if (!ok) {
        return;
      }
      this.userService.activate(user.id).subscribe({
        next: () => {
          this.snackBar.open('Usuario activado correctamente', 'Cerrar', { duration: 3000 });
          this.loadUsers();
        },
        error: (error) => this.showBackendError(error, 'No se pudo activar el usuario'),
      });
    });
  }

  deactivate(user: UserListItem): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Desactivar usuario',
        message: 'El usuario no podrá iniciar sesión mientras esté inactivo.',
        confirmText: 'Desactivar',
        danger: true,
      } as ConfirmDialogData,
      autoFocus: false,
      width: '26rem',
      maxWidth: '92vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((ok) => {
      if (!ok) {
        return;
      }
      this.userService.deactivate(user.id).subscribe({
        next: () => {
          this.snackBar.open('Usuario desactivado correctamente', 'Cerrar', { duration: 3000 });
          this.loadUsers();
        },
        error: (error) => this.showBackendError(error, 'No se pudo desactivar el usuario'),
      });
    });
  }

  confirmDelete(user: UserListItem): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Eliminar usuario',
        message:
          'El usuario será eliminado del sistema, pero se conservará su historial para auditoría.',
        confirmText: 'Eliminar',
        danger: true,
      } as ConfirmDialogData,
      autoFocus: false,
      width: '26rem',
      maxWidth: '92vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((ok) => {
      if (!ok) {
        return;
      }
      this.userService.delete(user.id).subscribe({
        next: () => {
          this.snackBar.open('Usuario eliminado correctamente', 'Cerrar', { duration: 3000 });
          this.loadUsers();
        },
        error: (error) => this.showBackendError(error, 'No se pudo eliminar el usuario'),
      });
    });
  }

  private openWithDetail(id: string, mode: 'edit' | 'view'): void {
    this.userService.getById(id).subscribe({
      next: (user: UserDetail) => this.openDialog({ mode, user }),
      error: () => this.snackBar.open('No se pudo cargar el usuario', 'Cerrar', { duration: 4000 }),
    });
  }

  private openDialog(data: { mode: UserDialogMode; user?: UserDetail }): void {
    const ref = this.dialog.open(UserFormDialogComponent, {
      data: data as UserDialogData,
      autoFocus: false,
      width: '680px',
      maxWidth: '94vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.loadUsers();
      }
    });
  }

  // ---------------------------------------------------------------
  // Roles
  // ---------------------------------------------------------------
  loadRoles(): void {
    this.loadingRoles.set(true);
    this.roleService.list().subscribe({
      next: (list) => {
        this.roles.set(list);
        this.loadingRoles.set(false);
      },
      error: () => {
        this.loadingRoles.set(false);
        this.snackBar.open('No se pudieron cargar los roles', 'Cerrar', { duration: 4000 });
      },
    });
  }

  openRoleDetail(role: Role): void {
    this.dialog.open(RoleDetailDialogComponent, {
      data: { role } as RoleDetailDialogData,
      autoFocus: false,
      width: '30rem',
      maxWidth: '94vw',
      panelClass: 'app-dialog',
    });
  }

  private showBackendError(error: unknown, fallback: string): void {
    const err = error as { error?: { message?: string } };
    this.snackBar.open(err?.error?.message ?? fallback, 'Cerrar', { duration: 4500 });
  }
}
