package com.tiendadebarrio.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PurchasesBySupplierResponse {

    private final UUID supplierId;
    private final String supplierName;
    private final Long purchaseCount;
    private final BigDecimal totalAmount;
}
