package com.tiendadebarrio.reports.dto;

import com.tiendadebarrio.cash.entity.CashMovementType;
import com.tiendadebarrio.common.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CashByCategoryResponse {

    private final CashMovementType movementType;
    private final String category;
    private final PaymentMethod paymentMethod;
    private final Long movementCount;
    private final BigDecimal totalAmount;
}
