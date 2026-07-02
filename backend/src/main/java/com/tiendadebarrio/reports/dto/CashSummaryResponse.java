package com.tiendadebarrio.reports.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class CashSummaryResponse {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final BigDecimal totalCashIncome;
    private final BigDecimal totalTransferIncome;
    private final BigDecimal totalCardIncome;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpenses;
    private final BigDecimal netAmount;
}
