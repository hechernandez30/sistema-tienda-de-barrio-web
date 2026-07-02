package com.tiendadebarrio.reports.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class DailySalesResponse {

    private final LocalDate date;
    private final Long salesCount;
    private final BigDecimal totalAmount;
}
