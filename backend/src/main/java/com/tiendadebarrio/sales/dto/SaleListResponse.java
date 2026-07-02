package com.tiendadebarrio.sales.dto;

import com.tiendadebarrio.common.enums.PaymentMethod;
import com.tiendadebarrio.sales.entity.SaleStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SaleListResponse {

    private final UUID id;
    private final Long saleNumber;
    private final String customerName;
    private final String cashierName;
    private final LocalDateTime saleDate;
    private final PaymentMethod paymentMethod;
    private final BigDecimal total;
    private final SaleStatus status;
}
