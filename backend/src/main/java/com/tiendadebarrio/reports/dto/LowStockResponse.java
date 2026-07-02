package com.tiendadebarrio.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class LowStockResponse {

    private final UUID productId;
    private final String barcode;
    private final String productName;
    private final BigDecimal currentStock;
    private final BigDecimal minStock;
    private final String categoryName;
}
