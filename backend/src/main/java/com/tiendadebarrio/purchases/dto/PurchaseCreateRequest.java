package com.tiendadebarrio.purchases.dto;

import com.tiendadebarrio.common.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PurchaseCreateRequest {

    @NotNull(message = "El proveedor es obligatorio")
    private UUID supplierId;

    private LocalDateTime purchaseDate;

    private Boolean isPaid;

    private PaymentMethod paymentMethod;

    private String notes;

    @NotEmpty(message = "La compra debe incluir al menos un producto")
    @Valid
    private List<PurchaseItemRequest> items;
}
