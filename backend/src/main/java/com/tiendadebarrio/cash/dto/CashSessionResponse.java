package com.tiendadebarrio.cash.dto;

import com.tiendadebarrio.cash.entity.CashSessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CashSessionResponse {

    private final UUID id;
    private final UUID openedBy;
    private final UUID closedBy;
    private final LocalDateTime openedAt;
    private final LocalDateTime closedAt;
    private final BigDecimal openingAmount;
    private final BigDecimal expectedAmount;
    private final BigDecimal countedAmount;
    private final BigDecimal differenceAmount;
    private final CashSessionStatus status;
    private final String notes;
}
