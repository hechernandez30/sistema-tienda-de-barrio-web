package com.tiendadebarrio.reports.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class PurchasesSummaryResponse {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final Long purchaseCount;
    private final Long confirmedPurchaseCount;
    private final Long cancelledPurchaseCount;
    private final BigDecimal totalPurchasedAmount;
}
