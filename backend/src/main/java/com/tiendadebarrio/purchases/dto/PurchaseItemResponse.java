package com.tiendadebarrio.purchases.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class PurchaseItemResponse {

    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final String barcode;
    private final BigDecimal quantity;
    private final BigDecimal unitCost;
    private final BigDecimal lineTotal;
    private final LocalDate expirationDate;
    private final String lotCode;
}
