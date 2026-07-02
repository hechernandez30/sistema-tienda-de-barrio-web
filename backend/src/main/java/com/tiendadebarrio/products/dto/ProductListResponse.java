package com.tiendadebarrio.products.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ProductListResponse {

    private final UUID id;
    private final String barcode;
    private final String sku;
    private final String name;
    private final String categoryName;
    private final String unitMeasureName;
    private final BigDecimal salePrice;
    private final BigDecimal currentStock;
    private final BigDecimal minStock;
    private final boolean active;
}
