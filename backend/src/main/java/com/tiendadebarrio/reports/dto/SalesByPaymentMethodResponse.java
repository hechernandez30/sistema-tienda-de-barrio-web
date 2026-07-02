package com.tiendadebarrio.reports.dto;

import com.tiendadebarrio.common.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SalesByPaymentMethodResponse {

    private final PaymentMethod paymentMethod;
    private final Long salesCount;
    private final BigDecimal totalAmount;
}
