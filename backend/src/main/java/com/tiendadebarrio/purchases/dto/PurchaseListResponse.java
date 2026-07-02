package com.tiendadebarrio.purchases.dto;

import com.tiendadebarrio.purchases.entity.PurchaseStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PurchaseListResponse {

    private final UUID id;
    private final Long purchaseNumber;
    private final String supplierName;
    private final LocalDateTime purchaseDate;
    private final PurchaseStatus status;
    private final boolean paid;
    private final BigDecimal total;
    private final LocalDateTime createdAt;
}
