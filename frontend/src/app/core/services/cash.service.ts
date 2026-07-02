import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, catchError, of, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CashMovement,
  CashSession,
  CloseCashSessionRequest,
  CreateCashMovementRequest,
  CurrentCashSummary,
  OpenCashSessionRequest,
} from '../models/cash.model';

@Injectable({ providedIn: 'root' })
export class CashService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/cash`;

  openSession(payload: OpenCashSessionRequest): Observable<CashSession> {
    return this.http.post<CashSession>(`${this.baseUrl}/sessions/open`, payload);
  }

  closeSession(id: string, payload: CloseCashSessionRequest): Observable<CashSession> {
    return this.http.post<CashSession>(`${this.baseUrl}/sessions/${id}/close`, payload);
  }

  /**
   * Resumen de la caja actualmente abierta. El backend responde 404 cuando no hay
   * caja abierta; en ese caso emitimos `null` para que la UI muestre el estado vacío.
   * El POS también usará este método para validar si puede vender.
   */
  getCurrentSession(): Observable<CurrentCashSummary | null> {
    return this.http.get<CurrentCashSummary>(`${this.baseUrl}/sessions/current`).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 404) {
          return of(null);
        }
        return throwError(() => error);
      }),
    );
  }

  listSessions(): Observable<CashSession[]> {
    return this.http.get<CashSession[]>(`${this.baseUrl}/sessions`);
  }

  getSession(id: string): Observable<CashSession> {
    return this.http.get<CashSession>(`${this.baseUrl}/sessions/${id}`);
  }

  getSessionMovements(id: string): Observable<CashMovement[]> {
    return this.http.get<CashMovement[]>(`${this.baseUrl}/sessions/${id}/movements`);
  }

  registerMovement(payload: CreateCashMovementRequest): Observable<CashMovement> {
    return this.http.post<CashMovement>(`${this.baseUrl}/movements`, payload);
  }

  listMovements(page = 0, size = 200): Observable<CashMovement[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<CashMovement[]>(`${this.baseUrl}/movements`, { params });
  }
}
