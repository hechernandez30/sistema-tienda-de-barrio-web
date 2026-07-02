package com.tiendadebarrio.products.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ProductDetailResponse {

    private final UUID id;
    private final String barcode;
    private final String sku;
    private final String name;
    private final String description;
    private final UUID categoryId;
    private final String categoryName;
    private final UUID unitMeasureId;
    private final String unitMeasureName;
    private final BigDecimal purchasePrice;
    private final BigDecimal salePrice;
    private final BigDecimal minStock;
    private final BigDecimal currentStock;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
