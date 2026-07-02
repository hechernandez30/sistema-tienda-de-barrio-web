import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Category,
  CategoryCreatePayload,
  UnitMeasure,
  UnitMeasureCreatePayload,
} from '../models/catalog.model';

/** Catálogos de apoyo para productos: categorías y unidades de medida. */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly categoriesUrl = `${environment.apiUrl}/categories`;
  private readonly unitMeasuresUrl = `${environment.apiUrl}/unit-measures`;

  listCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(this.categoriesUrl);
  }

  createCategory(payload: CategoryCreatePayload): Observable<Category> {
    return this.http.post<Category>(this.categoriesUrl, payload);
  }

  listUnitMeasures(): Observable<UnitMeasure[]> {
    return this.http.get<UnitMeasure[]>(this.unitMeasuresUrl);
  }

  createUnitMeasure(payload: UnitMeasureCreatePayload): Observable<UnitMeasure> {
    return this.http.post<UnitMeasure>(this.unitMeasuresUrl, payload);
  }
}
