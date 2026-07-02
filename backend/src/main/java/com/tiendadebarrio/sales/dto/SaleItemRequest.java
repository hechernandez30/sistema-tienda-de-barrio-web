package com.tiendadebarrio.sales.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class SaleItemRequest {

    @NotNull(message = "El producto es obligatorio")
    private UUID productId;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad debe ser mayor que cero")
    private BigDecimal quantity;

    /**
     * Precio unitario opcional enviado por el frontend. En esta primera versión se ignora
     * y siempre se usa products.sale_price para evitar manipulación de precios.
     */
    private BigDecimal unitPrice;
}
