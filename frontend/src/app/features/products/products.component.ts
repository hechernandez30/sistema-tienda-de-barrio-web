import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ProductService } from '../../core/services/product.service';
import { ProductDetail, ProductListItem } from '../../core/models/product.model';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../shared/components/confirm-dialog/confirm-dialog.component';
import {
  ProductDialogData,
  ProductFormDialogComponent,
} from './product-form-dialog/product-form-dialog.component';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './products.component.html',
})
export class ProductsComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly products = signal<ProductListItem[]>([]);
  readonly loading = signal(false);
  readonly searchTerm = signal('');
  readonly searchControl = new FormControl('', { nonNullable: true });

  readonly filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const list = this.products();
    if (!term) {
      return list;
    }
    return list.filter((p) =>
      [p.name, p.sku, p.barcode]
        .filter((value): value is string => !!value)
        .some((value) => value.toLowerCase().includes(term)),
    );
  });

  readonly stats = computed(() => {
    const list = this.products();
    return {
      total: list.length,
      active: list.filter((p) => p.active).length,
      inactive: list.filter((p) => !p.active).length,
      lowStock: list.filter((p) => this.isLowStock(p)).length,
    };
  });

  constructor() {
    // Se configura en el contexto de inyección (constructor) para poder usar takeUntilDestroyed().
    this.searchControl.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((value) => this.searchTerm.set(value));
  }

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading.set(true);
    this.productService.list().subscribe({
      next: (products) => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('No se pudieron cargar los productos', 'Cerrar', { duration: 4000 });
      },
    });
  }

  isLowStock(product: ProductListItem): boolean {
    return Number(product.currentStock) <= Number(product.minStock);
  }

  openCreate(): void {
    const ref = this.dialog.open(ProductFormDialogComponent, {
      data: { mode: 'create' } as ProductDialogData,
      autoFocus: false,
      width: '640px',
      maxWidth: '94vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.loadProducts();
      }
    });
  }

  openEdit(product: ProductListItem): void {
    this.openWithDetail(product.id, 'edit');
  }

  openView(product: ProductListItem): void {
    this.openWithDetail(product.id, 'view');
  }

  confirmDelete(product: ProductListItem): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Eliminar producto',
        message: `¿Deseas eliminar "${product.name}"? Esta acción realiza un borrado lógico.`,
        confirmText: 'Eliminar',
        danger: true,
      } as ConfirmDialogData,
      autoFocus: false,
      width: '26rem',
      maxWidth: '92vw',
      panelClass: 'app-dialog',
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.productService.delete(product.id).subscribe({
        next: () => {
          this.snackBar.open('Producto eliminado correctamente', 'Cerrar', { duration: 3000 });
          this.loadProducts();
        },
        error: (error) => {
          const message = error?.error?.message ?? 'No se pudo eliminar el producto';
          this.snackBar.open(message, 'Cerrar', { duration: 4000 });
        },
      });
    });
  }

  private openWithDetail(id: string, mode: 'edit' | 'view'): void {
    this.productService.getById(id).subscribe({
      next: (product: ProductDetail) => {
        const ref = this.dialog.open(ProductFormDialogComponent, {
          data: { mode, product } as ProductDialogData,
          autoFocus: false,
          width: '640px',
          maxWidth: '94vw',
          panelClass: 'app-dialog',
        });
        ref.afterClosed().subscribe((saved) => {
          if (saved) {
            this.loadProducts();
          }
        });
      },
      error: () => {
        this.snackBar.open('No se pudo cargar el producto', 'Cerrar', { duration: 4000 });
      },
    });
  }
}
