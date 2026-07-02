package com.tiendadebarrio.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class TopProductResponse {

    private final UUID productId;
    private final String barcode;
    private final String productName;
    private final BigDecimal quantitySold;
    private final BigDecimal totalAmount;
}
