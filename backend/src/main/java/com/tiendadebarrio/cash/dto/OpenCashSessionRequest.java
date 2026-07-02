package com.tiendadebarrio.cash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OpenCashSessionRequest {

    @NotNull(message = "El monto de apertura es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto de apertura no puede ser negativo")
    private BigDecimal openingAmount;

    private String notes;
}
