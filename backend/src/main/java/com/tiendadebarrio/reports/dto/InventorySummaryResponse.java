package com.tiendadebarrio.reports.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InventorySummaryResponse {

    private final Long totalProducts;
    private final Long activeProducts;
    private final Long lowStockProducts;
    private final BigDecimal totalInventoryCost;
    private final BigDecimal totalInventorySaleValue;
}
