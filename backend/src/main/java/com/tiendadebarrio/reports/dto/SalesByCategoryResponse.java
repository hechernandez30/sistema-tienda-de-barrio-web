package com.tiendadebarrio.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class SalesByCategoryResponse {

    private final UUID categoryId;
    private final String categoryName;
    private final BigDecimal quantitySold;
    private final BigDecimal totalAmount;
    private final Long lineCount;
}
