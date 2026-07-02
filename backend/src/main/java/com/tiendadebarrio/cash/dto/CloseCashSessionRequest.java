package com.tiendadebarrio.cash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CloseCashSessionRequest {

    @NotNull(message = "El monto contado es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto contado no puede ser negativo")
    private BigDecimal countedAmount;

    private String notes;
}
