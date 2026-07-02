package com.tiendadebarrio.cash.dto;

import com.tiendadebarrio.cash.entity.CashMovementType;
import com.tiendadebarrio.common.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CashMovementRequest {

    private UUID cashSessionId;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private CashMovementType movementType;

    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 80, message = "La categoría no puede exceder 80 caracteres")
    private String category;

    @NotNull(message = "El método de pago es obligatorio")
    private PaymentMethod paymentMethod;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor que cero")
    private BigDecimal amount;

    private String description;
}
