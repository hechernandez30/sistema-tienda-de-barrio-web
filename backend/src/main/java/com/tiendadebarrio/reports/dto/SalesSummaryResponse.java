package com.tiendadebarrio.reports.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class SalesSummaryResponse {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final BigDecimal totalSales;
    private final Long salesCount;
    private final Long cancelledSalesCount;
    private final BigDecimal averageSaleAmount;
}
