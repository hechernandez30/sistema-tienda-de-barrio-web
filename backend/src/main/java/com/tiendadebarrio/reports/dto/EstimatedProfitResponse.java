package com.tiendadebarrio.reports.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Utilidad ESTIMADA del período. El costo se calcula con el products.purchase_price ACTUAL,
 * no con el costo histórico exacto de cada venta, por lo que puede variar si el precio de
 * compra cambia después de registrada la venta.
 */
@Getter
@Builder
public class EstimatedProfitResponse {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final BigDecimal totalSales;
    private final BigDecimal estimatedCost;
    private final BigDecimal estimatedGrossProfit;
}
