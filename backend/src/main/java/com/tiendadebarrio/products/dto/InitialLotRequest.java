package com.tiendadebarrio.products.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class InitialLotRequest {

    @NotNull(message = "La cantidad del lote es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad debe ser mayor que cero")
    private BigDecimal quantity;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate expirationDate;

    private String lotCode;
}
