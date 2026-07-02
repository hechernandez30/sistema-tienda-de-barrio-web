package com.tiendadebarrio.inventory.dto;

import com.tiendadebarrio.inventory.entity.InventoryMovementType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class InventoryMovementResponse {

    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final String barcode;
    private final InventoryMovementType movementType;
    private final BigDecimal quantity;
    private final BigDecimal previousStock;
    private final BigDecimal newStock;
    private final BigDecimal unitCost;
    private final String notes;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
}
