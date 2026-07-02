package com.tiendadebarrio.cash.dto;

import com.tiendadebarrio.cash.entity.CashMovementType;
import com.tiendadebarrio.common.enums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CashMovementResponse {

    private final UUID id;
    private final UUID cashSessionId;
    private final CashMovementType movementType;
    private final String category;
    private final PaymentMethod paymentMethod;
    private final BigDecimal amount;
    private final String description;
    private final UUID referenceSaleId;
    private final UUID referencePurchaseId;
    private final LocalDateTime createdAt;
    private final UUID createdBy;
}
