package com.tiendadebarrio.sales.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class SaleItemResponse {

    private final UUID productId;
    private final String barcode;
    private final String productName;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal discountAmount;
    private final BigDecimal lineTotal;
}
