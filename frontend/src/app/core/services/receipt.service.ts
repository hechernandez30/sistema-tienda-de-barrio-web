import { Injectable } from '@angular/core';

import { PAYMENT_METHOD_LABELS, SaleDetail } from '../models/sale.model';

/**
 * Genera un comprobante de compra-venta imprimible a partir del detalle de una venta.
 * Abre una ventana con el ticket formateado y dispara el diálogo de impresión del
 * navegador, que también permite "Guardar como PDF".
 */
@Injectable({ providedIn: 'root' })
export class ReceiptService {
  private readonly storeName = 'Variedades Hernández';

  print(sale: SaleDetail): boolean {
    const win = window.open('', '_blank', 'width=380,height=640');
    if (!win) {
      return false;
    }
    win.document.open();
    win.document.write(this.buildHtml(sale));
    win.document.close();
    return true;
  }

  private buildHtml(sale: SaleDetail): string {
    const number = sale.saleNumber != null ? `#${sale.saleNumber}` : '';
    const date = this.formatDate(sale.saleDate);
    const cashier = sale.cashier?.fullName || sale.cashier?.username || '—';
    const customerName = sale.customer?.fullName || 'Consumidor final';
    const customerNit = sale.customer?.nit ? ` · NIT: ${this.escape(sale.customer.nit)}` : '';
    const payment = PAYMENT_METHOD_LABELS[sale.paymentMethod] ?? sale.paymentMethod;
    const cancelledBadge =
      sale.status === 'CANCELLED' ? `<div class="cancelled">VENTA ANULADA</div>` : '';
    // URL absoluta del logo (la ventana de impresión es un documento aparte).
    const logoUrl = `${window.location.origin}/logo.png`;

    const rows = sale.items
      .map(
        (item) => `
          <tr>
            <td class="name">
              ${this.escape(item.productName)}
              <span class="qtline">${this.formatQty(item.quantity)} x Q ${this.money(item.unitPrice)}</span>
            </td>
            <td class="amount">Q ${this.money(item.lineTotal)}</td>
          </tr>`,
      )
      .join('');

    const discountRow =
      sale.discountTotal > 0
        ? `<div class="line"><span>Descuento</span><span>- Q ${this.money(sale.discountTotal)}</span></div>`
        : '';
    const taxRow =
      sale.taxTotal > 0
        ? `<div class="line"><span>Impuesto</span><span>Q ${this.money(sale.taxTotal)}</span></div>`
        : '';

    return `<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8" />
<title>Comprobante ${this.escape(number)}</title>
<style>
  * { box-sizing: border-box; }
  html, body { margin: 0; padding: 0; }
  body {
    font-family: 'Courier New', Courier, monospace;
    color: #000;
    background: #fff;
    font-size: 12px;
    padding: 12px 14px;
  }
  .center { text-align: center; }
  .logo { width: 68px; height: 68px; border-radius: 50%; object-fit: cover; margin: 0 auto 6px; display: block; }
  .store { font-size: 15px; font-weight: 700; }
  .subtitle { font-size: 11px; margin-top: 2px; }
  .divider { border-top: 1px dashed #000; margin: 8px 0; }
  .meta { font-size: 11px; line-height: 1.5; }
  .meta strong { font-weight: 700; }
  table { width: 100%; border-collapse: collapse; }
  td { vertical-align: top; padding: 3px 0; }
  td.name { padding-right: 6px; }
  td.amount { text-align: right; white-space: nowrap; font-weight: 700; }
  .qtline { display: block; font-size: 10px; color: #333; }
  .totals { margin-top: 6px; font-size: 12px; }
  .line { display: flex; justify-content: space-between; padding: 2px 0; }
  .total { font-size: 15px; font-weight: 700; border-top: 1px solid #000; margin-top: 4px; padding-top: 6px; }
  .foot { margin-top: 12px; font-size: 11px; }
  .cancelled {
    text-align: center; font-weight: 700; color: #b00020;
    border: 1px solid #b00020; padding: 4px; margin: 8px 0; letter-spacing: 1px;
  }
  @media print { body { padding: 0; } }
</style>
</head>
<body onload="window.focus(); window.print();">
  <div class="center">
    <img class="logo" src="${logoUrl}" alt="${this.escape(this.storeName)}" onerror="this.style.display='none'" />
    <div class="store">${this.escape(this.storeName)}</div>
    <div class="subtitle">Comprobante de compra-venta</div>
  </div>

  ${cancelledBadge}

  <div class="divider"></div>

  <div class="meta">
    <div><strong>Comprobante:</strong> ${this.escape(number || 'S/N')}</div>
    <div><strong>Fecha:</strong> ${this.escape(date)}</div>
    <div><strong>Atendió:</strong> ${this.escape(cashier)}</div>
    <div><strong>Cliente:</strong> ${this.escape(customerName)}${customerNit}</div>
    <div><strong>Pago:</strong> ${this.escape(payment)}</div>
  </div>

  <div class="divider"></div>

  <table>
    <tbody>
      ${rows}
    </tbody>
  </table>

  <div class="divider"></div>

  <div class="totals">
    <div class="line"><span>Subtotal</span><span>Q ${this.money(sale.subtotal)}</span></div>
    ${discountRow}
    ${taxRow}
    <div class="line total"><span>TOTAL</span><span>Q ${this.money(sale.total)}</span></div>
  </div>

  <div class="divider"></div>

  <div class="center foot">
    ¡Gracias por su compra!<br />
    Este comprobante no es una factura fiscal.
  </div>
</body>
</html>`;
  }

  private money(value: number): string {
    return (value ?? 0).toLocaleString('es-GT', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  }

  private formatQty(value: number): string {
    return (value ?? 0).toLocaleString('es-GT', { maximumFractionDigits: 3 });
  }

  private formatDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString('es-GT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  private escape(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }
}
