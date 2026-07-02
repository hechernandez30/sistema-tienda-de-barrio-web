package com.tiendadebarrio.purchases.dto;

import com.tiendadebarrio.common.enums.PaymentMethod;
import com.tiendadebarrio.purchases.entity.PurchaseStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class PurchaseResponse {

    private final UUID id;
    private final Long purchaseNumber;
    private final SupplierSummary supplier;
    private final LocalDateTime purchaseDate;
    private final PurchaseStatus status;
    private final boolean paid;
    private final PaymentMethod paymentMethod;
    private final BigDecimal subtotal;
    private final BigDecimal discountTotal;
    private final BigDecimal taxTotal;
    private final BigDecimal total;
    private final String notes;
    private final List<PurchaseItemResponse> items;
    private final LocalDateTime createdAt;

    @Getter
    @Builder
    public static class SupplierSummary {
        private final UUID id;
        private final String name;
        private final String nit;
    }
}
