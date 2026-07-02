package com.tiendadebarrio.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class InventoryAdjustmentRequest {

    @NotNull(message = "El producto es obligatorio")
    private UUID productId;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad debe ser mayor que cero")
    private BigDecimal quantity;

    @DecimalMin(value = "0.0", message = "El costo unitario no puede ser negativo")
    private BigDecimal unitCost;

    private String notes;
}
