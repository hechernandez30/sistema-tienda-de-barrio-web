package com.tiendadebarrio.inventory.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ProductStockResponse {

    private final UUID productId;
    private final String barcode;
    private final String name;
    private final BigDecimal currentStock;
    private final BigDecimal minStock;
    private final String unitMeasureName;
    private final boolean active;
}
