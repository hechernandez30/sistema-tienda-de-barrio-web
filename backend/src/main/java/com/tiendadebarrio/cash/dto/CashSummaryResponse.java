package com.tiendadebarrio.cash.dto;

import com.tiendadebarrio.cash.entity.CashSessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CashSummaryResponse {

    private final UUID cashSessionId;
    private final CashSessionStatus status;
    private final LocalDateTime openedAt;
    private final UUID openedBy;
    private final BigDecimal openingAmount;
    private final BigDecimal totalCashIncome;
    private final BigDecimal totalTransferIncome;
    private final BigDecimal totalCardIncome;
    private final BigDecimal totalExpenses;
    private final BigDecimal expectedAmount;
    private final long movementCount;
}
