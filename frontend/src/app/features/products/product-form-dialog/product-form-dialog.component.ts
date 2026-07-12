import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ProductService } from '../../../core/services/product.service';
import { CatalogService } from '../../../core/services/catalog.service';
import {
  ProductCreatePayload,
  ProductDetail,
  ProductUpdatePayload,
} from '../../../core/models/product.model';
import { Category, UnitMeasure } from '../../../core/models/catalog.model';
import {
  QuickCreateDialogComponent,
  QuickCreateDialogData,
} from '../../../shared/components/quick-create-dialog/quick-create-dialog.component';

export type ProductDialogMode = 'create' | 'edit' | 'view';

export interface ProductDialogData {
  mode: ProductDialogMode;
  product?: ProductDetail;
}

export interface InitialLotRow {
  quantity: number;
  expirationDate: string;
  lotCode: string;
}

@Component({
  selector: 'app-product-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './product-form-dialog.component.html',
})
export class ProductFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly productService = inject(ProductService);
  private readonly catalogService = inject(CatalogService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<ProductFormDialogComponent>);
  readonly data = inject<ProductDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly isView = this.data.mode === 'view';
  readonly isEdit = this.data.mode === 'edit';
  readonly isCreate = this.data.mode === 'create';

  readonly categories = signal<Category[]>([]);
  readonly unitMeasures = signal<UnitMeasure[]>([]);
  readonly initialLots = signal<InitialLotRow[]>([]);
  /** Signal propio: FormControl.value no invalida computed de Angular. */
  readonly tracksExpirationEnabled = signal(false);

  readonly title = computed(() => {
    switch (this.data.mode) {
      case 'create':
        return 'Nuevo producto';
      case 'edit':
        return 'Editar producto';
      default:
        return 'Detalle del producto';
    }
  });

  readonly form = this.fb.nonNullable.group({
    barcode: ['', [Validators.required, Validators.maxLength(80)]],
    sku: ['', [Validators.maxLength(80)]],
    name: ['', [Validators.required, Validators.maxLength(160)]],
    description: [''],
    categoryId: [''],
    unitMeasureId: [''],
    purchasePrice: [0, [Validators.required, Validators.min(0)]],
    salePrice: [0, [Validators.required, Validators.min(0)]],
    minStock: [0, [Validators.required, Validators.min(0)]],
    currentStock: [0, [Validators.min(0)]],
    tracksExpiration: [false],
    active: [true],
  });

  constructor() {
    const product = this.data.product;
    if (product) {
      this.form.patchValue({
        barcode: product.barcode,
        sku: product.sku ?? '',
        name: product.name,
        description: product.description ?? '',
        categoryId: product.categoryId ?? '',
        unitMeasureId: product.unitMeasureId ?? '',
        purchasePrice: product.purchasePrice,
        salePrice: product.salePrice,
        minStock: product.minStock,
        currentStock: product.currentStock,
        tracksExpiration: product.tracksExpiration,
        active: product.active,
      });
      this.tracksExpirationEnabled.set(product.tracksExpiration);
    }

    this.form.controls.tracksExpiration.valueChanges.subscribe((enabled) => {
      this.tracksExpirationEnabled.set(enabled);
      if (enabled && this.isCreate) {
        this.form.controls.currentStock.setValue(0);
        if (this.initialLots().length === 0) {
          this.addInitialLot();
        }
      } else if (!enabled) {
        this.initialLots.set([]);
      }
    });

    this.loadCatalogs();

    // El stock actual se ajusta desde Inventario: sólo editable al crear.
    if (this.isEdit) {
      this.form.controls.currentStock.disable();
      if (product && (product.tracksExpiration || product.currentStock > 0)) {
        this.form.controls.tracksExpiration.disable();
      }
    }
    if (this.isView) {
      this.form.disable();
    }
  }

  /** Carga categorías y unidades activas. Conserva el valor actual del producto aunque
   *  ya no esté activo, para no perder la selección al editar/ver. */
  private loadCatalogs(): void {
    const product = this.data.product;

    this.catalogService.listCategories().subscribe((categories) => {
      let list = categories;
      if (product?.categoryId && !list.some((c) => c.id === product.categoryId)) {
        list = [
          { id: product.categoryId, name: product.categoryName ?? 'Categoría actual', active: true },
          ...list,
        ];
      }
      this.categories.set(list);
    });

    this.catalogService.listUnitMeasures().subscribe((units) => {
      let list = units;
      if (product?.unitMeasureId && !list.some((u) => u.id === product.unitMeasureId)) {
        list = [
          {
            id: product.unitMeasureId,
            code: '',
            name: product.unitMeasureName ?? 'Unidad actual',
            active: true,
          },
          ...list,
        ];
      }
      this.unitMeasures.set(list);
    });
  }

  /** Abre el formulario rápido para crear una categoría y la deja seleccionada. */
  addCategory(): void {
    const ref = this.dialog.open(QuickCreateDialogComponent, {
      data: {
        title: 'Nueva categoría',
        subtitle: 'Se agregará al catálogo y quedará seleccionada.',
        icon: 'category',
        fields: [{ key: 'name', label: 'Nombre', required: true, maxLength: 100, placeholder: 'Ej: Bebidas' }],
      } as QuickCreateDialogData,
      autoFocus: false,
      width: '440px',
      maxWidth: '96vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((result?: Record<string, string>) => {
      if (!result?.['name']) {
        return;
      }
      this.catalogService.createCategory({ name: result['name'] }).subscribe({
        next: (category) => {
          this.categories.update((list) => [category, ...list.filter((c) => c.id !== category.id)]);
          this.form.controls.categoryId.setValue(category.id);
        },
        error: (error) =>
          this.snackBar.open(error?.error?.message ?? 'No se pudo crear la categoría', 'Cerrar', {
            duration: 4000,
          }),
      });
    });
  }

  /** Abre el formulario rápido para crear una unidad de medida y la deja seleccionada. */
  addUnitMeasure(): void {
    const ref = this.dialog.open(QuickCreateDialogComponent, {
      data: {
        title: 'Nueva unidad de medida',
        subtitle: 'Se agregará al catálogo y quedará seleccionada.',
        icon: 'straighten',
        fields: [
          { key: 'code', label: 'Código', required: true, maxLength: 20, placeholder: 'Ej: UND', uppercase: true },
          { key: 'name', label: 'Nombre', required: true, maxLength: 80, placeholder: 'Ej: Unidad' },
        ],
      } as QuickCreateDialogData,
      autoFocus: false,
      width: '440px',
      maxWidth: '96vw',
      panelClass: 'app-dialog',
    });
    ref.afterClosed().subscribe((result?: Record<string, string>) => {
      if (!result?.['code'] || !result?.['name']) {
        return;
      }
      this.catalogService.createUnitMeasure({ code: result['code'], name: result['name'] }).subscribe({
        next: (unit) => {
          this.unitMeasures.update((list) => [unit, ...list.filter((u) => u.id !== unit.id)]);
          this.form.controls.unitMeasureId.setValue(unit.id);
        },
        error: (error) =>
          this.snackBar.open(error?.error?.message ?? 'No se pudo crear la unidad de medida', 'Cerrar', {
            duration: 4000,
          }),
      });
    });
  }

  addInitialLot(): void {
    this.initialLots.update((rows) => [
      ...rows,
      { quantity: 1, expirationDate: '', lotCode: '' },
    ]);
  }

  removeInitialLot(index: number): void {
    this.initialLots.update((rows) => rows.filter((_, i) => i !== index));
  }

  updateInitialLot(index: number, field: keyof InitialLotRow, value: string): void {
    this.initialLots.update((rows) =>
      rows.map((row, i) => {
        if (i !== index) {
          return row;
        }
        if (field === 'quantity') {
          const quantity = Number(value);
          return { ...row, quantity: Number.isFinite(quantity) ? quantity : 0 };
        }
        return { ...row, [field]: value };
      }),
    );
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

    if (this.isCreate && this.form.controls.tracksExpiration.value) {
      const lots = this.initialLots();
      if (lots.length === 0) {
        this.snackBar.open('Agrega al menos un lote inicial con fecha de vencimiento', 'Cerrar', {
          duration: 4000,
        });
        return;
      }
      const invalidLot = lots.some((lot) => lot.quantity <= 0 || !lot.expirationDate);
      if (invalidLot) {
        this.snackBar.open('Cada lote debe tener cantidad mayor que cero y fecha de vencimiento', 'Cerrar', {
          duration: 4000,
        });
        return;
      }
    }

    this.saving.set(true);
    const raw = this.form.getRawValue();
    const request$ =
      this.data.mode === 'create'
        ? this.productService.create(this.buildCreatePayload(raw))
        : this.productService.update(this.data.product!.id, this.buildUpdatePayload(raw));

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open(
          this.data.mode === 'create' ? 'Producto creado correctamente' : 'Producto actualizado correctamente',
          'Cerrar',
          { duration: 3000 },
        );
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.saving.set(false);
        const message = error?.error?.message ?? 'No se pudo guardar el producto';
        this.snackBar.open(message, 'Cerrar', { duration: 4000 });
      },
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  /** Bloquea notación científica y signos en los campos numéricos (e, E, +, -). */
  blockInvalidNumberKey(event: KeyboardEvent): void {
    if (['e', 'E', '+', '-'].includes(event.key)) {
      event.preventDefault();
    }
  }

  /** Selecciona el contenido al enfocar para que el 0 inicial se reemplace al escribir. */
  selectOnFocus(event: FocusEvent): void {
    (event.target as HTMLInputElement | null)?.select();
  }

  private buildCreatePayload(raw: ReturnType<typeof this.form.getRawValue>): ProductCreatePayload {
    const tracksExpiration = raw.tracksExpiration;
    const payload: ProductCreatePayload = {
      barcode: raw.barcode.trim(),
      sku: this.emptyToNull(raw.sku),
      name: raw.name.trim(),
      description: this.emptyToNull(raw.description),
      categoryId: this.emptyToNull(raw.categoryId),
      unitMeasureId: this.emptyToNull(raw.unitMeasureId),
      purchasePrice: Number(raw.purchasePrice),
      salePrice: Number(raw.salePrice),
      minStock: Number(raw.minStock),
      tracksExpiration,
      active: raw.active,
    };

    if (tracksExpiration) {
      payload.initialLots = this.initialLots().map((lot) => ({
        quantity: lot.quantity,
        expirationDate: lot.expirationDate,
        lotCode: this.emptyToNull(lot.lotCode),
      }));
    } else {
      payload.currentStock = Number(raw.currentStock);
    }

    return payload;
  }

  private buildUpdatePayload(raw: ReturnType<typeof this.form.getRawValue>): ProductUpdatePayload {
    return {
      barcode: raw.barcode.trim(),
      sku: this.emptyToNull(raw.sku),
      name: raw.name.trim(),
      description: this.emptyToNull(raw.description),
      categoryId: this.emptyToNull(raw.categoryId),
      unitMeasureId: this.emptyToNull(raw.unitMeasureId),
      purchasePrice: Number(raw.purchasePrice),
      salePrice: Number(raw.salePrice),
      minStock: Number(raw.minStock),
      tracksExpiration: raw.tracksExpiration,
      active: raw.active,
    };
  }

  private emptyToNull(value: string): string | null {
    const trimmed = value?.trim();
    return trimmed ? trimmed : null;
  }
}
