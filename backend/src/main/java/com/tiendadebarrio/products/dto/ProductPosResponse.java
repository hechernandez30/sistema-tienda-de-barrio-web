package com.tiendadebarrio.products.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ProductPosResponse {

    private final UUID id;
    private final String barcode;
    private final String name;
    private final BigDecimal salePrice;
    private final BigDecimal currentStock;
    private final String unitMeasureName;
}
