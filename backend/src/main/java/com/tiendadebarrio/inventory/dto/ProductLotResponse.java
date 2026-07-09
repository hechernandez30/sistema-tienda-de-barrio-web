package com.tiendadebarrio.inventory.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class ProductLotResponse {

    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final String barcode;
    private final String lotCode;
    private final LocalDate expirationDate;
    private final long daysToExpire;
    private final BigDecimal quantity;
    private final BigDecimal unitCost;
    private final boolean expired;
}
