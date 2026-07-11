package com.tiendadebarrio.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ventas agrupadas por categoría de producto. El costo usa el purchase_price actual
 * del producto (utilidad estimada), igual que el reporte general de utilidad.
 */
@Getter
@AllArgsConstructor
public class SalesByCategoryResponse {

    private final UUID categoryId;
    private final String categoryName;
    private final BigDecimal quantitySold;
    private final BigDecimal totalAmount;
    private final BigDecimal estimatedCost;
    private final BigDecimal estimatedGrossProfit;
    private final Long lineCount;
}
