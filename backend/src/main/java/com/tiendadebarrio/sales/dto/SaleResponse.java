package com.tiendadebarrio.sales.dto;

import com.tiendadebarrio.common.enums.PaymentMethod;
import com.tiendadebarrio.sales.entity.SaleStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class SaleResponse {

    private final UUID id;
    private final Long saleNumber;
    private final CustomerSummary customer;
    private final CashierSummary cashier;
    private final UUID cashSessionId;
    private final LocalDateTime saleDate;
    private final PaymentMethod paymentMethod;
    private final BigDecimal subtotal;
    private final BigDecimal discountTotal;
    private final BigDecimal taxTotal;
    private final BigDecimal total;
    private final SaleStatus status;
    private final String notes;
    private final List<SaleItemResponse> items;
    private final LocalDateTime createdAt;

    @Getter
    @Builder
    public static class CustomerSummary {
        private final UUID id;
        private final String fullName;
        private final String nit;
    }

    @Getter
    @Builder
    public static class CashierSummary {
        private final UUID id;
        private final String username;
        private final String fullName;
    }
}
